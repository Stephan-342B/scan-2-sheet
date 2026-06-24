package com.mahefa.ocr.scan2sheet.common.properties;

import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "application")
@Component
public class AppProperties {

    private Batch batch;
    private BackgroundTask backgroundTask;
    private Dir dir;
    private File file;
    private Table table;

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public BackgroundTask getBackgroundTask() {
        return backgroundTask;
    }

    public void setBackgroundTask(BackgroundTask backgroundTask) {
        this.backgroundTask = backgroundTask;
    }

    public Dir getDir() {
        return dir;
    }

    public void setDir(Dir dir) {
        this.dir = dir;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public static class Batch {

        private int chunkSize;

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }
    }

    public static class BackgroundTask {

        private int corePoolSize;
        private int queueCapacity;
        private String threadNamePrefix;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    public static class Dir {
        private String source;
        private String archive;
        private String output;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getArchive() {
            return archive;
        }

        public void setArchive(String archive) {
            this.archive = archive;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }
    }

    public static class File {
        private String allowedExt;
        private Reader reader;
        private Writer writer;

        public String getAllowedExt() {
            return allowedExt;
        }

        public void setAllowedExt(String allowedExt) {
            this.allowedExt = allowedExt;
        }

        public Reader getReader() {
            return reader;
        }

        public void setReader(Reader reader) {
            this.reader = reader;
        }

        public Writer getWriter() {
            return writer;
        }

        public void setWriter(Writer writer) {
            this.writer = writer;
        }

        public static class Reader {
            private String limit;

            public String getLimit() {
                return limit;
            }

            public void setLimit(String limit) {
                this.limit = limit;
            }
        }

        public static class Writer {
            private WriterMode writingMode;
            private int chunkSize;

            public WriterMode getWritingMode() {
                return writingMode;
            }

            public void setWritingMode(WriterMode writingMode) {
                this.writingMode = writingMode;
            }

            public int getChunkSize() {
                return chunkSize;
            }

            public void setChunkSize(int chunkSize) {
                this.chunkSize = chunkSize;
            }
        }
    }

    public static class Table {
        private Row row;
        private Col col;

        public Row getRow() {
            return row;
        }

        public void setRow(Row row) {
            this.row = row;
        }

        public Col getCol() {
            return col;
        }

        public void setCol(Col col) {
            this.col = col;
        }

        public static class Row {

            private boolean headerPresent = true;
            private String select;
            private int maxCount = 1000;

            public boolean isHeaderPresent() {
                return headerPresent;
            }

            public void setHeaderPresent(boolean headerPresent) {
                this.headerPresent = headerPresent;
            }

            public String getSelect() {
                return select;
            }

            public void setSelect(String select) {
                this.select = select;
            }

            public int getMaxCount() {
                return maxCount;
            }

            public void setMaxCount(int maxCount) {
                this.maxCount = maxCount;
            }
        }

        public static class Col {
            private String range;
            private String select;

            public String getRange() {
                return range;
            }

            public void setRange(String range) {
                this.range = range;
            }

            public String getSelect() {
                return select;
            }

            public void setSelect(String select) {
                this.select = select;
            }
        }
    }
}
