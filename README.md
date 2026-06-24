# scan-2-sheet

A Spring Batch application that extracts tabular data from scanned/low-quality images and
writes the result to **Google Sheets** or an **Excel** workbook. It uses OpenCV to locate the
table's columns from the header row, crops each cell, and runs **Tesseract** (via Tess4J) OCR
on it.

- **OCR:** Tess4J 5.8.0 (Tesseract LSTM engine)
- **Image processing:** OpenCV 4.7.0 (org.openpnp)
- **Writers:** Google Sheets API, Apache POI (XLSX)
- **Runtime:** Java 21, Spring Boot 3.5 / Spring Batch 5

---

## How it works

1. Each image in the source directory is loaded as an OpenCV `Mat`.
2. `processHeader` detects the column positions from the header row.
3. The image is sliced into rows; each cell is cropped and OCR'd.
4. Rows are written to the configured target (Google Sheet or Excel).

OCR runs in parallel across a thread pool (`AsyncItemProcessor`), while the reader stays
single-threaded so **row order is preserved**. Each worker thread uses its own Tesseract
instance (Tesseract is not thread-safe).

---

## Prerequisites

- **Java 21** (JDK)
- **Maven** (or use the bundled `./mvnw` wrapper)
- **Tesseract trained data** — the `tessdata-best/` directory containing `eng.traineddata` and
  `osd.traineddata`. These are large (~40 MB) and are **not** committed to the repo; download
  them from [tessdata_best](https://github.com/tesseract-ocr/tessdata_best) and place them in
  `tessdata-best/` at the project root (or point `tess4j.data.path` elsewhere).
- For Google Sheets output: a **Google Cloud service account** with the Sheets & Drive APIs
  enabled (see below).

---

## Quick start (local)

```bash
# 1. Put the Tesseract data in place (see Prerequisites)
# 2. Put your Google service-account key at:
#    src/main/resources/scan2sheet-fx-295559181df6.json   (see "Google Sheets setup")
# 3. Create the working directories referenced in application.yml:
mkdir -p ~/Scan2Sheet/data/{source,archive,output}
# 4. Drop image(s) into the source dir (optionally inside a sub-folder = "document"):
#    ~/Scan2Sheet/data/source/myfile.png
# 5. Run it:
./mvnw spring-boot:run
```

Or build a jar and run it:

```bash
./mvnw clean package
java -jar target/scan-2-sheet-0.0.1-SNAPSHOT.jar
```

The job runs once on startup, processes every image under `dir.source`, moves processed files
to `dir.archive`, and exits.

> **Note:** input files may sit directly in the source directory or inside a sub-folder. A
> sub-folder name is treated as the **document id** (a Google spreadsheet tab grouping / an
> Excel workbook); loose files in the root are grouped under a generated `scan-2-sheet_*` id.

---

## Configuration reference (`src/main/resources/application.yml`)

| Key | Meaning |
|-----|---------|
| `application.batch.chunk-size` | Spring Batch chunk size (rows committed per chunk). |
| `application.background-task.core-pool-size` | OCR worker threads (parallelism). Each thread loads its own ~31 MB Tesseract model. |
| `application.background-task.queue-capacity` | Bounded work queue; back-pressure beyond this (CallerRunsPolicy). |
| `application.background-task.thread-name-prefix` | Worker thread name prefix (e.g. `s2s`). |
| `application.dir.source` | Input images directory. |
| `application.dir.archive` | Where processed images are moved. |
| `application.dir.output` | Output directory (used by the Excel writer). |
| `application.file.allowed-ext` | Allowed image extensions. |
| `application.file.writer.writing-mode` | `GOOGLE_SHEET` or `XLS`. (`CSV` is declared but **not implemented**.) |
| `application.table.row.header-present` | Whether the first row is a header. |
| `application.table.row.select` | Rows to extract: `NONE` (all) or a range like `1-3,7,1000`. |
| `application.table.row.max-count` | Max rows per file (default 1000) — used to compute the row pitch. |
| `application.table.col.select` | Columns to extract: `NONE` (all) or a range like `9-11,16`. |
| `tess4j.data.path` | Absolute path to the `tessdata-best` directory. |
| `tess4j.engine.mode` | Tesseract OCR engine mode (`3` = LSTM). |
| `tess4j.engine.psm` | Page segmentation mode (`7` = single line; good for single-cell crops). |
| `google-api.application-name` | Google API client app name. |
| `google-api.spreadsheet-id` | Target Google spreadsheet id. |

Any property can be overridden at runtime via environment variables or `--key=value`, e.g.:

```bash
java -jar target/scan-2-sheet-0.0.1-SNAPSHOT.jar \
  --application.dir.source=/data/in \
  --google-api.spreadsheet-id=YOUR_SHEET_ID
```

---

## Google Sheets setup

Authentication uses a **service account** key loaded from the classpath. The code currently
expects the file `src/main/resources/scan2sheet-fx-295559181df6.json`
(see `GoogleSheetAPI#getService`). To use your own account, either name your key file the same,
or change that filename in `GoogleSheetAPI`.

### Local

1. In the [Google Cloud Console](https://console.cloud.google.com/), create (or pick) a project
   and **enable the Google Sheets API and Google Drive API**.
2. Create a **service account** and download its **JSON key**.
3. Save the key at `src/main/resources/scan2sheet-fx-295559181df6.json`.
4. **Share your target spreadsheet with the service account's email**
   (`...@<project>.iam.gserviceaccount.com`) as an **Editor** — otherwise writes are denied.
5. Put the spreadsheet id (the long id in the sheet URL) in `google-api.spreadsheet-id`.

> **Limitation:** because this uses the free tier, **one spreadsheet id is used at a time**.
> To target a different spreadsheet, change `google-api.spreadsheet-id` and re-run.

### Production

- **Do not bake the key into the image/jar.** Keep the JSON out of source control (it is
  gitignored). Provide it at deploy time, e.g. mount it as a secret/volume into
  `src/main/resources/` on the classpath, or set
  `GOOGLE_APPLICATION_CREDENTIALS` and adapt `GoogleSheetAPI` to use Application Default
  Credentials.
- Externalize `google-api.spreadsheet-id` and the `application.dir.*` paths via environment
  variables (`GOOGLE_API_SPREADSHEET_ID`, `APPLICATION_DIR_SOURCE`, …) rather than editing the
  jar.
- Rotate the service-account key if it was ever committed or shared.

### Excel instead of Google Sheets

Set `application.file.writer.writing-mode: XLS`. Output `.xlsx` files are written under
`application.dir.output`. No Google credentials are needed in this mode.

---

## Performance / tuning

OCR dominates runtime (one `doOCR` call per cell). Parallelism is controlled by
`application.background-task.core-pool-size`; raise it to use more cores (each worker adds
~31 MB of Tesseract model memory). `application.batch.chunk-size` controls how many rows are
buffered/committed per chunk. Row order is preserved regardless of worker count.

To restrict work (and speed up runs), use `application.table.row.select` /
`application.table.col.select` to extract only specific rows/columns.

---

## Troubleshooting

- **`Service account JSON not found` / permission denied** — ensure the key file is on the
  classpath and the target sheet is **shared with the service-account email**.
- **Empty/garbled OCR** — verify `tess4j.data.path` points at `tessdata-best`, and that the
  source images are the expected resolution. Debug crops can be dumped via the (commented)
  `imwrite` calls in `ImageProcessing` / `ColumnExtractor`.
- **Job didn't run** — files must match `application.file.allowed-ext` and live under
  `application.dir.source`.

---

## Project layout

```
src/main/java/com/mahefa/ocr/scan2sheet/
├── Scan2SheetApplication.java        # entry point
├── conf/                             # Job, batch infra, Tesseract, thread pool config
├── common/                           # properties, tools (ImageProcessing, ImageConverter)
├── data/                             # RowContext, ColumnContext, TableStructure, properties
└── services/
    ├── api/GoogleSheetAPI.java       # Google Sheets/Drive calls + credential loading
    ├── job/pipeline/                 # RowReader / RowProcessor / RowWriter
    ├── job/extractor/                # RowExtractor / ColumnExtractor (OCR)
    ├── job/delegate/                 # multi-resource reader + per-file delegate
    └── job/listener/                 # step/job lifecycle listeners
```
