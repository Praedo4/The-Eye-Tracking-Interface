package sample;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RList;
import org.rosuda.JRI.Rengine;
import sample.ET.ETCollection;
import sample.ET.GazePoint;
import sample.FS.FSCollection;
import sample.FS.FSEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SignalProcessing {
    private Rengine re;
    int bb_x, bb_y, bb_w, bb_h ;

    public SignalProcessing(){
        String args[] = null;
        re=new Rengine(args, false, new TextConsole());
        re.eval("require(emov)");
    }

    public void println(String text){
        //System.out.println(text);
    }

    public boolean withinBoundingBox(double x, double y){
        return (x >= bb_x && y >= bb_y && x<= (bb_x + bb_w) && y <= (bb_y + bb_h));
    }

    public ETCollection cutOutliers(ETCollection data, int bounding_box_x,int bounding_box_y, int bounding_box_width, int bounding_box_height) {

        ETCollection result = new ETCollection();
        ArrayList<GazePoint> samplesWithinBB = new ArrayList<GazePoint>();
        bb_x = bounding_box_x;
        bb_y = bounding_box_y;
        bb_w = bounding_box_width;
        bb_h = bounding_box_height;
        for (GazePoint sample : data.gazePoints) {
            if (withinBoundingBox(sample.x, sample.y))
                samplesWithinBB.add(sample);
        }

        GazePoint[] samplesArray = (GazePoint[]) samplesWithinBB.toArray(new GazePoint[samplesWithinBB.size()]);
        result.initialize(samplesArray);
        return result;
    }


    public ETCollection runInterpolation(ETCollection data, int minGapSize, int maxGapSize) {
        int totalMissingSamples = 0;
        long totalTime = 0;
        for (int i = 1; i < data.size; i++) {
            GazePoint pt = data.gazePoints[i];
            GazePoint prevPt = data.gazePoints[i - 1];
            int missingSamples = (int) (pt.timestamp - prevPt.timestamp) / 15000;
            if (missingSamples > minGapSize && missingSamples < maxGapSize) {

                println("Missing samples: " + missingSamples);
                totalMissingSamples += missingSamples;
            }
            if (missingSamples > minGapSize)
                totalTime += pt.timestamp - prevPt.timestamp;
        }
        println("Total missing samples: " + totalMissingSamples);
        GazePoint[] newRawData = new GazePoint[data.size + totalMissingSamples];
        int missingSamplesAdded = 0;
        newRawData[0] = data.gazePoints[0];
        for (int i = 1; i < data.size; i++) {
            GazePoint pt = data.gazePoints[i];
            GazePoint prevPt = data.gazePoints[i - 1];
            long gapSize = pt.timestamp - prevPt.timestamp;
            int missingSamples = (int) (gapSize / 15000);
            if (missingSamples > minGapSize && missingSamples < maxGapSize) {
                for (int j = 1; j <= missingSamples; j++) {
                    int sampleDuration = (int) (gapSize / missingSamples);
                    double newX = prevPt.x + (pt.x - prevPt.x) * (j * 1.0 / missingSamples);
                    double newY = prevPt.y + (pt.y - prevPt.y) * (j * 1.0 / missingSamples);
                    double newDiameter = prevPt.pupil_diameter + (pt.pupil_diameter - prevPt.pupil_diameter) * (j * 1.0 / missingSamples);
                    long newTimestamp = prevPt.timestamp + sampleDuration * j;
                    long newDuration = sampleDuration;
                    GazePoint newPt = new GazePoint(newX, newY, newTimestamp, newDuration, newDiameter);
                    newRawData[i + missingSamplesAdded] = newPt;
                    missingSamplesAdded++;
                    println("Added #" + missingSamplesAdded + "at (" + newX + "," + newY + ") ts:" + newTimestamp + " duration: " + newDuration);
                }
            }
            newRawData[i + missingSamplesAdded] = data.gazePoints[i];
        }
        ETCollection out_data = new ETCollection();
        out_data.initialize(newRawData);
        println("Interpolation complete");
        return out_data;
    }

    public ETCollection applySavitzkyGolayFilter(ETCollection data, int sgolayOrder, int sgolayLength){
        int n = data.size;
        int[] timestamps = new int[n];
        long timeStampStart;
        double xs[] = new double[n], ys[] = new double[n];
        timeStampStart = data.gazePoints[0].timestamp;
        for (int i = 0; i < n; i++) {
            GazePoint pt = data.gazePoints[i];
            timestamps[i] = (int) (pt.timestamp - timeStampStart);
            xs[i] = pt.x;
            ys[i] = pt.y;
        }

        REXP rResult;
        // Pass the data to R
        if (re.assign("ts", timestamps) && re.assign("xs", xs) && re.assign("ys", ys)) {
            // Run Savitzky-Golay smoothing filter
            rResult = re.eval("library(signal, warn.conflicts = FALSE)");
            rResult = re.eval("filter(sgolay(p=" + sgolayOrder + ", n=" + sgolayLength + ", m=0), xs)");
            if (rResult == null) {
                re.eval("install.packages(\'signal\')");
                rResult = re.eval("require(signal)");
                rResult = re.eval("filter(sgolay(p=" + sgolayOrder + ", n=" + sgolayLength + ", m=0), xs)");
             }

            double[] newXs = rResult.asDoubleArray();
            rResult = re.eval("filter(sgolay(p=" + sgolayOrder + ", n=" + sgolayLength + ", m=0), ys)");
            double[] newYs = rResult.asDoubleArray();

            ETCollection filteredData = new ETCollection();
            data.copyTo(filteredData);
            for (int i = 0; i < n; i++){
                filteredData.gazePoints[i].x = newXs[i];
                filteredData.gazePoints[i].y = newYs[i];
            }
            return  filteredData;
        }
        else {
            println("Failed to assign data to R");
            return data;
        }
    }

    public FSCollection runEventDetection(ETCollection data, double idtDispersion, int idtDuration) {

        int n = data.size;
        int[] timestamps = new int[n];
        long timeStampStart;
        double xs[] = new double[n], ys[] = new double[n];
        timeStampStart = data.gazePoints[0].timestamp;
        for (int i = 0; i < n; i++) {
            GazePoint pt = data.gazePoints[i];
            timestamps[i] = (int) (pt.timestamp - timeStampStart);
            xs[i] = pt.x;
            ys[i] = pt.y;
        }

        REXP rResult;
        // Pass the data to R
        if (re.assign("ts", timestamps) && re.assign("xs", xs) && re.assign("ys", ys)) {
            // Run IDT

            String callIDT = "emov.idt(ts,xs,ys," + idtDispersion + "," + idtDuration + ")";
            rResult = re.eval(callIDT);
            if (rResult == null) {
                println("rJava and/or emov package are missing. Launching installation process");
                re.eval("install.packages(\'rJava\')");
                re.eval("install.packages(\'emov\')");
                rResult = re.eval(callIDT);
            }

            // Get the data back from R
            RList l = rResult.asList();
            try {
                int starts[] = l.at(0).asIntArray();
                int ends[] = l.at(1).asIntArray();
                int durs[] = l.at(2).asIntArray();
                double out_xs[] = l.at(3).asDoubleArray();
                double out_ys[] = l.at(4).asDoubleArray();
                // Parse the data
                int numberOfFixations = starts.length;
                FSEvent[] fsData = new FSEvent[numberOfFixations];
                FSCollection eventData = new FSCollection();
                int j = 0;
                for (int i = 0; i < numberOfFixations; i++) {
                    //System.out.printf("Fixation #%d: starts at %d ends at %d (duration: %d) at position (%f, %f)\n", i + 1, starts[i], ends[i], durs[i], out_xs[i], out_ys[i]);
                    double dispersion = 0, mean_pupil_diameter = 0,number_of_samples = 0;

                    for (; j < n && data.gazePoints[j].timestamp <= timeStampStart + ends[i]; j++) {
                        if (data.gazePoints[j].timestamp >= timeStampStart + starts[i]) {
                            dispersion = Math.max(dispersion, Math.sqrt(Math.pow(data.gazePoints[j].x - out_xs[i], 2) + Math.pow(data.gazePoints[j].y - out_ys[i], 2)));
                            mean_pupil_diameter += data.gazePoints[j].pupil_diameter;
                            number_of_samples++;
                        }
                    }
                    fsData[i] = new FSEvent(out_xs[i], out_ys[i], dispersion, dispersion, timeStampStart + starts[i], timeStampStart + ends[i], durs[i], mean_pupil_diameter/number_of_samples, true);
                }
                eventData.initialize(fsData);
                // Visualize the data

                return eventData;
            }
            catch (Exception exception){
                println("IDT couldn't be run");
                return null;
            }
        } else {
            println("Failed to assign data to R");
            return null;
        }

    }
}
