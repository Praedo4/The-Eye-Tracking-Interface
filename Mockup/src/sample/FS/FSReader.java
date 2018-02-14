package sample.FS;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FSReader {
    public final int START = 4, END = 5,  DURATION = 6, POS_X = 7, POS_Y = 8, EVENT_TYPE = 0, EYE = 1, POS_X2 = 9, POS_Y2 = 10;

    public FSEvent readEvent(String line){
        String[] words = line.split("\\s");
        if(words.length <= EYE || words[EYE].compareTo("L") == 0)
            return null;
        if(words[EVENT_TYPE].compareTo("Fixation") == 0 ){
            double x = Double.parseDouble(words[POS_X]), y = Double.parseDouble(words[POS_Y]);
            double dispX = Double.parseDouble(words[POS_X2]), dispY = Double.parseDouble(words[POS_Y2]);
            long start = Long.parseLong(words[START]), end = Long.parseLong(words[END]), duration = Long.parseLong(words[DURATION]);
            return new FSEvent(x,y,dispX,dispY,start, end, duration, true);
        }
        else if(words[EVENT_TYPE].compareTo("Saccade") == 0 ){
            double x = Double.parseDouble(words[POS_X]), y = Double.parseDouble(words[POS_Y]);
            double x2 = Double.parseDouble(words[POS_X2]), y2 = Double.parseDouble(words[POS_Y2]);
            long start = Long.parseLong(words[START]), end = Long.parseLong(words[END]), duration = Long.parseLong(words[DURATION]);
            return new FSEvent(x,y,x2,y2,start, end, duration, false);
        }
        return null;
    }

    public FSCollection readFSCollection(String fileName){
        FSCollection collection = new FSCollection();
        ArrayList <FSEvent> events = new ArrayList<FSEvent>();
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            System.out.println("Reader initalized"); // File open

            String line = null;
            while((line = bufferedReader.readLine()) != null){
                FSEvent pt = readEvent(line);
                if(pt != null){
                    events.add(pt);
                }
            }
            FSEvent[] data = (FSEvent[])events.toArray(new FSEvent[events.size()]);
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
