package sample.ET;

public class GazePoint {
    public double x,y; // (x,y) coordinates on the screen
    public long timestamp; // Event start
    public long duration; // Event duration

    public void copyTo(GazePoint dest){
        dest.x = x;
        dest.y = y;
        dest.timestamp = timestamp;
        dest.duration = duration;
    }

    GazePoint(){
        x = y = timestamp = duration = -1;
    }

    public GazePoint(double cx, double cy, long ctm, long cdur){
        x = cx;
        y = cy;
        timestamp = ctm;
        duration = cdur;
    }

}
