package me.monoto.customseeds.crops;

public class CropData {
    private String cropType;
    private double progress;
    private int age;
    private boolean fullyGrown;

    public CropData(double progress, int age, boolean fullyGrown, String cropType) {
        this.progress = progress;
        this.age = age;
        this.fullyGrown = fullyGrown;
        this.cropType = cropType;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isFullyGrown() {
        return fullyGrown;
    }

    public void setFullyGrown(boolean fullyGrown) {
        this.fullyGrown = fullyGrown;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }
}

