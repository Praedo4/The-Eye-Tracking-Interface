package sample.ET;

public class GazePoint {
    public double x,y,pupil_diameter; // (x,y) coordinates on the screen
    public long timestamp; // Event start
    public long duration; // Event duration

    public void copyTo(GazePoint dest){
        dest.x = x;
        dest.y = y;
        dest.timestamp = timestamp;
        dest.duration = duration;
        dest.pupil_diameter = pupil_diameter;
    }

    GazePoint(){
        x = y = timestamp = duration = -1;
    }

    public GazePoint(double cx, double cy, long ctm, long cdur, double diameter){
        x = cx;
        y = cy;
        timestamp = ctm;
        duration = cdur;
        pupil_diameter = diameter;
    }

}
