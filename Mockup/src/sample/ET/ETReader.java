package sample.ET;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ETReader {

    public final int TIMESTAMP = 0, DURATION = 1, POS_L_X = 21, POS_L_Y = 22, POS_R_X = 23, POS_R_Y = 24, DIA_L = 9, DIA_R = 12, EVENT_TYPE = 54, STIMULUS_ID = 58, MODE_AVERAGE = 111, MODE_ONLY_LEFT = 112, MODE_ONLY_RIGHT = 113;
    public int mode = MODE_AVERAGE;
    public boolean REMOVE_NEGATIVES = true;

    public GazePoint[] readGazePoint(String line){
        String[] words = line.split("\\s");
        if(words[EVENT_TYPE].compareTo("Fixation") == 0 && words.length > STIMULUS_ID ){
            if(words[STIMULUS_ID].length() < 1 || Integer.parseInt(words[STIMULUS_ID]) < 2)
                return null;
            double l_x = Double.parseDouble(words[POS_L_X]), l_y = Double.parseDouble(words[POS_L_Y]);
            double r_x = Double.parseDouble(words[POS_R_X]), r_y = Double.parseDouble(words[POS_R_Y]);
            double l_d = Double.parseDouble(words[DIA_L]), r_d = Double.parseDouble(words[DIA_R]);
            long timestamp = Long.parseLong(words[TIMESTAMP]);
            int duration = DURATION;
            GazePoint[] pts = new GazePoint[2];
            pts[0]= new GazePoint(l_x,l_y,timestamp,duration,l_d);
            pts[1] = new GazePoint(r_x,r_y,timestamp,duration,r_d);
            return pts;
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

            return new GazePoint(x,y,timestamp, duration, 0);
        }
        return null;
    }

    private ArrayList <GazePoint> mergeBinocularData(ArrayList <GazePoint> left, ArrayList <GazePoint> right){
        ArrayList <GazePoint> gazePoints = new ArrayList<GazePoint>();
        int i = 0, j = 0;
        int n = left.size(), m = right.size();
        while(i < n || j < m){
            GazePoint leftPt = left.get(i), rightPt = right.get(j);
            GazePoint pt;
            if(leftPt.timestamp == rightPt.timestamp){
                pt = new GazePoint((leftPt.x + rightPt.x)/2.0, (leftPt.y + rightPt.y)/2.0,leftPt.timestamp, leftPt.duration, (leftPt.pupil_diameter + rightPt.pupil_diameter)/2.0);
                i++;
                j++;
            }
            else if(leftPt.timestamp < rightPt.timestamp){
                pt = leftPt;
                i++;
            }
            else{
                pt = rightPt;
                j++;
            }
            if(!REMOVE_NEGATIVES || pt.x > 0 && pt.y > 0)
                gazePoints.add(pt);
        }
        return gazePoints;
    }

    public ETCollection readETCollection(String fileName ){
        ETCollection collection = new ETCollection();
        ArrayList <GazePoint> l_gazePoints = new ArrayList<GazePoint>();
        ArrayList <GazePoint> r_gazePoints = new ArrayList<GazePoint>();
        ArrayList <GazePoint> gazePoints;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            System.out.println("Reader initalized"); // File open

            String line = null;
            while((line = bufferedReader.readLine()) != null){
                GazePoint[] pts = readGazePoint(line);
                if(pts != null){
                    l_gazePoints.add(pts[0]);
                    r_gazePoints.add(pts[1]);
                }
            }
            switch(mode){
                case MODE_AVERAGE:
                    gazePoints = mergeBinocularData(l_gazePoints,r_gazePoints);
                    break;
                case MODE_ONLY_LEFT:
                    gazePoints = l_gazePoints;
                    break;
                case MODE_ONLY_RIGHT:
                    gazePoints = r_gazePoints;
                    break;
                default:
                    gazePoints = null;
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
