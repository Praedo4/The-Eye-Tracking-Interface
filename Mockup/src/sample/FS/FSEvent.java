package sample.FS;

public class FSEvent {
    public double x,y; // (x,y) coordinates on the screen
    public double x2,y2; // Dispersion within fixation OR End of saccade
    public double meanPupilDiameter;
    public long start, end; // Event start and end
    public long duration; // Event duration
    public int type; // Fixation or Saccade
    public final int FIXATION = 0, SACCADE = 1;


    public void copyTo(FSEvent dest){
        dest.x = x;
        dest.y = y;
        dest.x2 = x2;
        dest.y2 = y2;
        dest.start = start;
        dest.end = end;
        dest.duration = duration;
        dest.type = type;
        dest.meanPupilDiameter = meanPupilDiameter;
    } 

    public FSEvent(){
        x = y = x2 = y2 = meanPupilDiameter = start = end =  duration = type  = -1;
    }

    public FSEvent(double cx, double cy, double cdx, double cdy, long cstart, long cend, long cdur, double cdiameter, boolean isFixation){
        x = cx;
        y = cy;
        x2 = cdx;
        y2 = cdy;
        start = cstart;
        end = cend;
        duration = cdur;
        meanPupilDiameter = cdiameter;
        type = isFixation? FIXATION : SACCADE;
    }
}
