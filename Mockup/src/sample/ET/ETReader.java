package sample.ET;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ETReader {

    public final int TIMESTAMP = 0, DURATION = 1, POS_X = 21, POS_Y = 22, EVENT_TYPE = 54, STIMULUS_ID = 58;

    public GazePoint readGazePoint(String line, int text_id){
        String[] words = line.split("\\s");
        if(words[EVENT_TYPE].compareTo("Fixation") == 0 && words.length > STIMULUS_ID ){
            if(words[STIMULUS_ID].length() < 1 || Integer.parseInt(words[STIMULUS_ID]) < 2 || (text_id != -1 && Integer.parseInt(words[STIMULUS_ID]) != text_id))
                return null;
            double x = Double.parseDouble(words[POS_X]), y = Double.parseDouble(words[POS_Y]);
            long timestamp = Long.parseLong(words[TIMESTAMP]);
            int duration = DURATION;

            //if(x > 0 && y > 0 && x < 1280 && y < 1028){
            //    GazePoint point = new GazePoint(x,y,timestamp, duration);
            //}
            //else{
            //System.out.println("Negative Fixation at " + x + "," + y);
            //}
            return new GazePoint(x,y,timestamp, duration);
        }
        return null;
    }

    public ETCollection readETCollection(String fileName, int text_id ){
        ETCollection collection = new ETCollection();
        ArrayList <GazePoint> gazePoints = new ArrayList<GazePoint>();
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            System.out.println("Reader initalized"); // File open

            String line = null;
            while((line = bufferedReader.readLine()) != null){
                GazePoint pt = readGazePoint(line, text_id);
                if(pt != null){
                    gazePoints.add(pt);
                }
            }
            GazePoint[] data = (GazePoint[])gazePoints.toArray(new GazePoint[gazePoints.size()]);
            collection.initialize(data);
            System.out.println("Reading complete; Collection size: " + data.length); // File read
            return collection;

        }
        catch(FileNotFoundException ex){
            System.out.println("Unable to open file '" + fileName + "'");
        }
        catch (IOException ex){
            System.out.println("Error reading file '" + fileName + "'");
        }
        return null;

    }
}
