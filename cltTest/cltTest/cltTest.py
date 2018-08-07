import numpy as np
import scipy.stats as stats
import matplotlib.pyplot as plt
import math
import random
from os import listdir
from os.path import isfile, join



path = 'data/'                                                  #path to the files
S = 500                                                         #number of samples to be drawn
testNs = [10,50,100,150,200,250,500]            #this is the set of Ns to be tested. In case of changing the size of the set, adjustments need to be made to ordering and number of subplots


random.seed()
pltIndex = 1                                                    
fig = plt.figure()                                              #initialize the figure
varianceList = []
for N in testNs:
    files = [f for f in listdir('data/') if isfile(join('data/', f))]
    print('# files: ' + str(N))

    meanList = []
    for i in range(0,N):
        f = len(files)
        j = random.randint(0,f-1)                               #draw a random file
        currentFile = []
        with open(path + files[j]) as file:
            lines = file.readlines()                            #read file
            for k in range(0,min(len(lines) - 1,S)):
                numberOfLines = len(lines)
                randomLine = random.randint(1,numberOfLines-1)  #draw a random sample
                splitLine = lines[randomLine].split()           
                currentFile.append(int(splitLine[11])/1000)          #add the drawn sample
                lines.pop(randomLine)                           #remove the sample to avoid double drawing
        meanList.append(np.mean(currentFile))                   #calculate mean for the file
        #varianceList.append(np.var(currentFile))    
        files.pop(j)                                        #remove the file to avoid double drawing
    varianceList.append(np.var(meanList))                       #calculate variance for current N
    #print('Means : ')                                          #|\
    #for mean in meanList:                                      #|= This code is used for debugging purposes to check mean values as they are calculated
    #    print(str(mean))                                       #|/
 
       
    #n_b = int(int(1+1.3*math.log10(N)))                        #number of bins for the plot of means
    n_b = 20
    meanPlot = fig.add_subplot(4,3,pltIndex)                    #4 = number of columns, 3 = number of rows, pltIndex is 1-12, going row by row left to right
    meanPlot.hist(meanList, normed=True, bins=n_b)              #create histogram
    meanPlot.set_title('Mean Fixation Duration N = ' + str(N))  #||\
    meanPlot.set_xlim(0,700)                                    #||= Formatting
    meanPlot.set_xlabel('Mean Fixation Duration, ms')               #||= 
    meanPlot.set_ylabel('Density')                              #||/
    #varPlot = fig.add_subplot(4,4, 4 + pltIndex)                                                          #|This code 
    pltIndex += 1                                                                                          #|
    #if pltIndex == 5:                                                                                     #|was used
    #    pltIndex += 4                                                                                     #|to create 
    #varPlot.hist(varianceList, normed=True, bins = n_b)#bins=int(1.5*n_b) + int(0.001*(n_b*n_b)))         #|plots of variance
    #varPlot.set_title('Fixation Duration Variance N = ' + str(N))                                         #|for each value of N
    #varPlot.set_xlim(0,200000000000)                                                                      #|instead of variance
    #fig.subplots_adjust(left=None, bottom=None, right=None, top=0.95, wspace=None, hspace=0.25)           #|of mean values
    print('_____________')

n_b = int(int(4+3.3*math.log10(len(varianceList))))             #number of bins for the plot of variance
varPlot = fig.add_subplot(4,3, 12)
#varPlot.hist(varianceList, normed=True, bins = n_b)#bins=int(1.5*n_b) + int(0.001*(n_b*n_b)))             #This code was used for creating histogram for variance instead of scatter plot
plt.scatter(testNs,varianceList)                                #create scatter plot
varPlot.set_title('Mean Fixation Duration Variance')            #|\
varPlot.set_xlabel('N')                                         #|= Formatting
varPlot.set_ylabel('Variance')                                  #|/    
#varPlot.set_xlim(0,25000000000)
fig.subplots_adjust(left=None, bottom=0.05, right=None, top=0.95, wspace=None, hspace=0.30) #figure formatting to fit all plots and labels nicely
plt.show()                                                      #display created figure
