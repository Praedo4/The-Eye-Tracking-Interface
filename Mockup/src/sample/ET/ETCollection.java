package sample.ET;

public class ETCollection {
    public GazePoint gazePoints[];
    public int size;


    public void copyTo(ETCollection dest){
        dest.size = size;
        dest.gazePoints = new GazePoint[size];
        for(int i = 0; i < size; i ++){
            dest.gazePoints[i] = new GazePoint();
            gazePoints[i].copyTo(dest.gazePoints[i]);

        }
    }

    public void initialize(GazePoint data[]){
        size = data.length;
        gazePoints = new GazePoint[size];
        for(int i = 0; i < size; i ++){
            gazePoints[i] = new GazePoint();
            data[i].copyTo(gazePoints[i]);

        }
    }

}
