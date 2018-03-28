package sample.ET;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ETReader {

    public final int TIMESTAMP = 0, DURATION = 1, POS_X = 21, POS_Y = 22, EVENT_TYPE = 54, STIMULUS_ID = 58;

    public GazePoint readGazePoint(String line){
        String[] words = line.split("\\s");
        if(words[EVENT_TYPE].compareTo("Fixation") == 0 && words.length > STIMULUS_ID ){
            if(words[STIMULUS_ID].length() < 1 || Integer.parseInt(words[STIMULUS_ID]) < 2)
                return null;
            double x = Double.parseDouble(words[POS_X]), y = Double.parseDouble(words[POS_Y]);
            long timestamp = Long.parseLong(words[TIMESTAMP]);
            int duration = DURATION;

            return new GazePoint(x,y,timestamp, duration);
        }
        return null;
    }

    public GazePoint readTETGazePoint(String line){
        String[] words = line.split("}|\\|,\"|,|:|\"");
        //String[] words = line.split("{\"|\":\"|\",\"|\":|,\"|");
        if(words.length > 31 ){
            double x = Double.parseDouble(words[47]), y = Double.parseDouble(words[51]);
            long timestamp = 100;
            int duration = DURATION;

            return new GazePoint(x,y,timestamp, duration);
        }
        return null;
    }

    public ETCollection readETCollection(String fileName ){
        ETCollection collection = new ETCollection();
        ArrayList <GazePoint> gazePoints = new ArrayList<GazePoint>();
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            System.out.println("Reader initalized"); // File open

            String line = null;
            while((line = bufferedReader.readLine()) != null){
                GazePoint pt = readGazePoint(line);
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

    public ETCollection readTETCollection(String fileName){
        ETCollection collection = new ETCollection();
        ArrayList <GazePoint> gazePoints = new ArrayList<GazePoint>();
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            System.out.println("Reader initalized"); // File open

            String line = null;
            while((line = bufferedReader.readLine()) != null){
                GazePoint pt = readTETGazePoint(line);
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

   //public void fixOffset(String fileName, Map<Integer, String> imageFilenames){
   //    try {
   //        FileReader fileReader = new FileReader(fileName);
   //        BufferedReader bufferedReader = new BufferedReader(fileReader);

   //        System.out.println("Reader initalized"); // File open

   //        String line = null;
   //        while((line = bufferedReader.readLine()) != null) {
   //            String[] words = line.split("\\s");
   //            if (words[EVENT_TYPE].compareTo("Fixation") == 0 && words.length > STIMULUS_ID) {
   //                if (words[STIMULUS_ID].length() < 1 || Integer.parseInt(words[STIMULUS_ID]) < 2 || (text_id != -1 && Integer.parseInt(words[STIMULUS_ID]) != text_id))
   //                    return;
   //                double x = Double.parseDouble(words[POS_X]), y = Double.parseDouble(words[POS_Y]);
   //                long timestamp = Long.parseLong(words[TIMESTAMP]);
   //                int duration = DURATION;

   //            }
   //        }

   //    }
   //    catch(FileNotFoundException ex){
   //        System.out.println("Unable to open file '" + fileName + "'");
   //    }
   //    catch (IOException ex){
   //        System.out.println("Error reading file '" + fileName + "'");
   //    }
   //}

}
