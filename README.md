# The-Eye-Tracking-Interface
Master thesis project

<< :  Mockup :  >>

To run project follow these steps:
1. Make sure you have JDK8 (64-bit) installed: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
2. Make sure you have R 3.4.3 (64-bit) installed: https://cran.r-project.org/bin/windows/base/old/3.4.3/
3. For the program to run, JRI is required. It is automatically installed when rJava is installed in R. To do that, we open R 3.4.3 (or RStudio if it runs that version of R) and run the command << install.packages('rJava') >>. This should install both rJava and JRI. If promt asks for usage of local directory (for user) - confirm.
4. To build a project, a java IDE is required. The one that was used when developing the project is 'IntelliJ IDEA Community Edition 2017.3.3', but any IDE can be used. However, following steps will refer to IntelliJ, for others refer to the same settings within the IDE.
5. Set up the proper Java version (8) for the project.
> Note: in case of following compilation error: 'java: invalid source release: 9', in IntelliJ proceed to 'File -> Project structure -> Project' in IntelliJ and set 'Project language level' to 8 and rebuild the project afterwards.
6. Go to run/debug configurations (in IntelliJ: Run -> Edit configurations -> Application -> Main) and set 'VM options' to be << -Djava.library.path="C:\Users\xxxxx\Documents\R\win-library\3.4\rJava\jri\x64" >>, where xxxxx is the username for which the R library was installed. Check the path to lead to the rJava\jri folder. This path might differ depending on the system. This example presents Windows 10 path.
7. In the run/debug configurations, set environment variable 'PATH' to be 'C:/Program Files/R/R-3.4.3/bin/x64'. This should be already done in the IntelliJ project file, that is already present for the project on the GitHub.
8. Build and run the program. First time that 'Run I-DT' button is pressed, there will be a promt message in case 'emov' package is required to be installed. In this case it will launch the installation an R package. Close the application and run it again for it to work correctly. Installation process is extremely inconsistent to control, but it should be promted when it's done.

After all of this, hopefully the project is up and running!
