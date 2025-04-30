package me.monoto.customseeds.crops;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class CropConfigData {
    private File file;
    private final YamlConfiguration config;

    public CropConfigData(File file, YamlConfiguration config) {
        this.file = file;
        this.config = config;
    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileNameWithoutExtension() {
        int index = this.file.getName().lastIndexOf('.');
        return (index > 0) ? this.file.getName().substring(0, index) : this.file.getName();
    }
}
