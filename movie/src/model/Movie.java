package model;



public class Movie {
    private int id;
    private String name;
    private String hall;
    private String showTime;
    private double price;
    private int totalSeats;
    private int availableSeats;
    private String imageUrl;
    private String videoObjectKey;
    private Integer durationSeconds;
    private String posterObjectKey;

    // Getter 和 Setter 方法
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHall() { return hall; }
    public void setHall(String hall) { this.hall = hall; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoObjectKey() { return videoObjectKey; }
    public void setVideoObjectKey(String v) { this.videoObjectKey = v; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer v) { this.durationSeconds = v; }

    public String getPosterObjectKey() { return posterObjectKey; }
    public void setPosterObjectKey(String v) { this.posterObjectKey = v; }

    /** 是否已上传可播放的视频 */
    public boolean hasVideo() {
        return videoObjectKey != null && !videoObjectKey.isEmpty();
    }
}
