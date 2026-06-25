package com.mahefa.ocr.scan2sheet.services.api;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.mahefa.ocr.scan2sheet.common.properties.GoogleApiProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetAPI {

    private final String APPLICATION_NAME;
    private final String SPREADSHEET_ID;

    private Sheets sheets;
    private Drive drive;

    public GoogleSheetAPI(GoogleApiProperties googleApiProperties) {
        this.APPLICATION_NAME = googleApiProperties.getApplicationName();
        this.SPREADSHEET_ID = googleApiProperties.getSpreadsheetId();
    }

    // Lazily build and cache the clients on first use. The service-account JSON is only required
    // when an operation actually talks to Google, so the Spring context can boot without it
    // (e.g. in CI, where the secret is not on the classpath).
    private Sheets sheets() throws IOException {
        if (sheets == null) {
            try {
                sheets = getService();
            } catch (GeneralSecurityException e) {
                throw new IOException("Failed to initialize Google Sheets client", e);
            }
        }
        return sheets;
    }

    private Drive drive() throws IOException {
        if (drive == null) {
            try {
                drive = getDriveService();
            } catch (GeneralSecurityException e) {
                throw new IOException("Failed to initialize Google Drive client", e);
            }
        }
        return drive;
    }

    public Spreadsheet newSpreadsheet(String title) throws IOException {
        // Step 1: Create spreadsheet file in target folder
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(title);
        fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        fileMetadata.setParents(Collections.singletonList("1Yfu7ZEkJz0N_WbLHxwbGMVDbamr9dPB_")); // folder id

        com.google.api.services.drive.model.File createdFile = drive().files()
                .create(fileMetadata)
                .setFields("id")
                .execute();

        String spreadsheetId = createdFile.getId();

        // Step 2: Share with user so they can see it
        Permission userPermission = new Permission()
                .setType("user")
                .setRole("writer")
                .setEmailAddress("r.mahefa.stephan@gmail.com");

        drive().permissions().create(spreadsheetId, userPermission)
                .setSendNotificationEmail(false)
                .execute();

        // Step 3: Fetch spreadsheet metadata
        Spreadsheet spreadsheet = sheets().spreadsheets()
                .get(spreadsheetId)
                .execute();

        return spreadsheet;
    }

    public Spreadsheet getDefaultSpreadsheet() {
        try {
            return sheets().spreadsheets().get(SPREADSHEET_ID).execute();
        } catch (Exception e) {
            return null;
        }
    }

    public Spreadsheet getSpreadsheet(String spreadSheetId) throws IOException {
        return sheets().spreadsheets().get(spreadSheetId).execute();
    }

    public Sheet getSheet(Spreadsheet spreadsheet, String title) throws IOException {
        return getDefaultSpreadsheet()
                .getSheets()
                .stream()
                .filter(sheet -> sheet.getProperties().getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    public Integer newSheet(Spreadsheet spreadsheet, String title) throws IOException {
        if (StringUtils.isBlank(title))
            return null;

        // Create a new sheet properties
        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(title); // Set the title of the new sheet

        // Create a request to add the new sheet
        Request addSheetRequest = new Request()
                .setAddSheet(new AddSheetRequest()
                        .setProperties(sheetProperties));

        // Create a batch update request to add the sheet
        List<Request> requests = new ArrayList<>();
        requests.add(addSheetRequest);

        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);

        // Execute the batch update request to add the new sheet
        BatchUpdateSpreadsheetResponse response = sheets().spreadsheets()
                .batchUpdate(spreadsheet.getSpreadsheetId(), batchUpdateRequest)
                .execute();

        // Get the ID of the newly created sheet
        return response.getReplies().get(0).getAddSheet().getProperties().getSheetId();
    }

    public void write(Spreadsheet spreadsheet, String title, List<List<Object>> data) throws IOException {
        if (CollectionUtils.isEmpty(data))
            return;

        ValueRange valueRange = new ValueRange()
                .setValues(data);

        sheets().spreadsheets().values()
                .append(spreadsheet.getSpreadsheetId(), "'" + title + "'!A1", valueRange)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .setIncludeValuesInResponse(true)
                .execute();
    }

    public void writeAll(Spreadsheet spreadsheet, Sheet sheet, List<List<Object>> data) throws IOException {
        if (CollectionUtils.isEmpty(data))
            return;

        List<Request> requests = new ArrayList<>();

        TextFormat textFormat = new TextFormat().setFontFamily("Times New Roman").setFontSize(12);
        CellFormat cellFormat = new CellFormat();
        cellFormat.setTextFormat(textFormat);
        cellFormat.setHorizontalAlignment("RIGHT");

        // Create requests for each row
        for (List<Object> row : data) {
            List<CellData> cells = new ArrayList<>();

            for (Object val : row) {
                if (val != null) {
                    cells.add(
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(val.toString()))
                                    .setUserEnteredFormat(cellFormat)
                    );
                }
            }

            requests.add(new Request()
                    .setAppendCells(new AppendCellsRequest()
                            .setSheetId(sheet.getProperties().getSheetId())
                            .setRows(Collections.singletonList(new RowData().setValues(cells)))
                            .setFields(
                                    "userEnteredValue,userEnteredFormat.textFormat.fontFamily,userEnteredFormat.textFormat.fontSize," +
                                    "userEnteredFormat.horizontalAlignment"
                            )
                    )
            );
        }

        // Execute the batch update
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        sheets().spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId(), batchUpdateRequest).execute();
    }

    public void autoResizeGrid(Spreadsheet spreadsheet, Sheet sheet, int lastIndex) throws IOException {
        // Create an AutoResizeDimensionsRequest
        AutoResizeDimensionsRequest autoResizeDimensionsRequest = new AutoResizeDimensionsRequest()
                .setDimensions(new DimensionRange()
                        .setSheetId(sheet.getProperties().getSheetId())
                        .setDimension("COLUMNS") // "ROWS" for rows, "COLUMNS" for columns
                        .setStartIndex(0) // Start index
                        .setEndIndex(lastIndex)); // End index

        // Add the request to a BatchUpdateSpreadsheetRequest
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(new Request().setAutoResizeDimensions(autoResizeDimensionsRequest)));

        // Call the batchUpdate method on the Sheets service
        Sheets.Spreadsheets.BatchUpdate batchUpdate = sheets().spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId(), batchUpdateRequest);
        batchUpdate.execute();
    }

    protected Sheets getService() throws IOException, GeneralSecurityException {
        InputStream inputStream = getClass().getResourceAsStream("/scan2sheet-fx-295559181df6.json");

        if (inputStream == null) {
            throw new IllegalStateException("Service account JSON not found in classpath");
        }

        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(inputStream)
                .createScoped(Arrays.asList(
                        SheetsScopes.SPREADSHEETS,
                        SheetsScopes.DRIVE
                ));

        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Drive getDriveService() throws IOException, GeneralSecurityException {
        InputStream inputStream = getClass().getResourceAsStream("/scan2sheet-fx-295559181df6.json");

        if (inputStream == null) {
            throw new IllegalStateException("Service account JSON not found in classpath");
        }

        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(inputStream)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/drive"));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Scan2Sheet")
                .build();
    }
}
