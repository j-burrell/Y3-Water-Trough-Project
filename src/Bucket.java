public class Bucket {

    private String bucketid;
    private int max_vol;
    private int current_vol;
    private int needed_to_fill_vol;

    public Bucket(String bucketid, int max_vol){

        this.bucketid = bucketid;
        this.max_vol = max_vol;
        this.current_vol = 0;
        this.needed_to_fill_vol = this.calculateNeedToFill();
    }

    //Calculate water needed to fill.
    private int calculateNeedToFill() {

        return this.max_vol - this.current_vol;
    }

    //Getters and setters.
    public String getBucketid() {
        return bucketid;
    }
    public void setBucketid(String bucketid) {
        this.bucketid = bucketid;
    }

    public int getMax_vol() {
        return max_vol;
    }
    public void setMax_vol(int max_vol) {
        this.max_vol = max_vol;
        this.setNeeded_to_fill();
    }

    public int getCurrent_vol() {
        return current_vol;
    }
    public void setCurrent_vol(int current_vol) {
        this.current_vol = current_vol;
        this.setNeeded_to_fill();
    }

    public int getNeeded_to_fill_vol() {
        return needed_to_fill_vol;
    }
    public void setNeeded_to_fill() {
        this.needed_to_fill_vol = calculateNeedToFill();
    }
}
