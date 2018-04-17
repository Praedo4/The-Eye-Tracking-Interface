package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.rosuda.JRI.*;

import java.util.Enumeration;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("The Eye-Trcaking Interface");
        primaryStage.setScene(new Scene(root, 1340, 950));
        primaryStage.setResizable(false);
        primaryStage.show();
    }



    public static void main(String[] args) {
//        Rengine re=new Rengine(args, false, new TextConsole());
//        //re.eval("print(1:10/3)");
//        //System.out.println(x=re.eval("iris"));
//        //re.eval("source(\"D:\\\\Coding\\\\Mockup\\\\r\\\\rtest1.R\")");
//        REXP result;

       // re.assign("x",result);
        //re.assign("x", "Hello");
        // result = re.eval("fixations");
        //System.out.print(result.asString());
        //System.out.println("Result: "+result.asString());
        //REXP x = re.eval("greeting ");
        //System.out.println("Greeting R: "+x.asString());
        //RList v = x.asList();
        //String[] l = v.keys();
        //for(String item : l){
        //    System.out.println(item);;
        //}
        // String s = x.asString();
        //System.out.println(s);
//        int bigN = 100;
//        int[] ts = new int[bigN];
//        double[] xs = new double[bigN];
//        double[] ys = new double[bigN];
//        for (int i = 0; i < bigN; i++){
//            ts[i] = i; xs[i] = i*1.0; ys[i] = i*1.0;}
//        if(re.assign("t",ts) && re.assign("x",xs) && re.assign("y", ys))
//        {
//            //re.eval("data#time <- x");
//            result = re.eval("print(t)");
//            result = re.eval("print(x)");
//            result = re.eval("print(y)");
//            re.eval("require(emov)");
//            result = re.eval("emov.idt(t,x,y,3,1)");
//            RList l = result.asList();
//            int starts[] = l.at(0).asIntArray();
//            int ends[] = l.at(1).asIntArray();
//            int durs[] = l.at(2).asIntArray();
//            double out_xs[] = l.at(3).asDoubleArray();
//            double out_ys[] = l.at(4).asDoubleArray();
//            for(int i = 0; i < starts.length; i++){
//                System.out.printf("Fixation #%d: starts at %d ends at %d (duration: %d) at position (%f, %f)\n",i+1,starts[i],ends[i],durs[i],out_xs[i],out_ys[i]);
//            }
//            //re.eval("print(fixations)");
//            //re.eval("")
//            //result = re.eval("data#time",true);
//            //int[] l = result.asIntArray();
////
//            //for(int i = 0; i < l.length; i++){
//            //    System.out.println(l[i]);
//            //}
//        }
//        else{
//            System.out.println("failed to assign");
//        }
        //if (v.getNames()!=null) {
        //    System.out.println("has names:");
        //    for (Enumeration e = v.getNames().elements(); e.hasMoreElements() ;) {
        //        System.out.println(e.nextElement());
        //    }
        //}
        launch(args);
    }
}
