package com.mahefa.ocr.scan2sheet.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties(prefix = "tess4j")
@Component
public class Tess4jProperties {

    private Conf conf;
    private Data data;
    private Engine engine;

    public Conf getConf() {
        return conf;
    }

    public void setConf(Conf conf) {
        this.conf = conf;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public static class Conf {
        private Var var;
        private TessVariable tessvariable;

        public Var getVar() {
            return var;
        }

        public void setVar(Var var) {
            this.var = var;
        }

        public TessVariable getTessvariable() {
            return tessvariable;
        }

        public void setTessvariable(TessVariable tessvariable) {
            this.tessvariable = tessvariable;
        }

        public static class Var {
            private String conf;

            public String getConf() {
                return conf;
            }

            public void setConf(String conf) {
                this.conf = conf;
            }
        }

        public static class TessVariable {
            private Map<String, String> options;

            public Map<String, String> getOptions() {
                return options;
            }

            public void setOptions(Map<String, String> options) {
                this.options = options;
            }
        }
    }

    public static class Data {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Engine {
        private int mode;
        private int psm = 7;

        public int getMode() {
            return mode;
        }

        public void setMode(int mode) {
            this.mode = mode;
        }

        public int getPsm() {
            return psm;
        }

        public void setPsm(int psm) {
            this.psm = psm;
        }
    }
}

