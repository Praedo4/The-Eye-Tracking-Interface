import numpy as np
import scipy.stats as stats
import matplotlib.pyplot as plt
import csv
import math
import random
import re
from os import listdir
from os.path import isfile, join


allfiles = [f for f in listdir('data/') if isfile(join('data/', f))]
fileset = []

fig = plt.figure()

mode = 0 #0 = USER 1 = TEXT
#PER USER
if mode == 0:
    for f in range(1,36):
        print('Evaluating user id#'+str(f))
        user_id = str(f)
        csvFile =  open('ks_user'+user_id+'.csv','w')
        if len(user_id) == 1:
            user_id = '0' + user_id
        user_id = 'p'+user_id+'+\w*'
        for file in allfiles:
            if re.match(user_id,file):
                fileset.append('data/' + file)
        readFiles = []
        for fileName in fileset:
            #print(fileName)
            currentFile = []
            with open(fileName, 'r') as file:
                next(file)
                for line in file:
                    splitLine = line.split()
                    currentFile.append(int(splitLine[11]))
            readFiles.append(currentFile)
        n = len(fileset)
        if n == 0:
            continue
        testResults = []
        statsResults = []
        pResults = []
        for i in range(0,n):
            for j in range(i+1, n):
                result = stats.ks_2samp(readFiles[i],readFiles[j])
                pResults.append(result[1])
                statsResults.append(result[0])
                #print(result)
        n_b = 100
       #pPlot = fig.add_subplot(4,4,f)
       #pPlot.hist(pResults, normed=True, bins=n_b)
       #pPlot.set_title('P value for user_id #' + str(f))
       #pPlot.set_xlim(0.01,1.1)
       #pPlot.set_ylim(0.0,1)
       #dPlot = fig.add_subplot(4,4,8 + f)
       #dPlot.hist(statsResults, normed=True, bins=n_b)
       #dPlot.set_title('D statistical value for user_id #' + str(f))
       #dPlot.set_xlim(0.01,1.1)

#Per text
if mode == 1:
    text_ids = [11299,13165,5938,11143,322,10879,4584,4504,6366,4701,6474,4094,7784,6046,2725,3775,9977,3819]
    T = len(text_ids)
    for t in range(1, 9):
        text_id = str(text_ids[t-1])
        print('Evaluating text id#'+str(text_ids[t-1]))
        text_id_re = '\w*'+str(text_id)+'+\w*'
        for file in allfiles:
            if re.match(text_id_re,file):
                fileset.append('data/' + file)
        readFiles = []
        for fileName in fileset:
            #print(fileName)
            currentFile = []
            with open(fileName, 'r') as file:
                next(file)
                for line in file:
                    splitLine = line.split()
                    currentFile.append(int(splitLine[11]))
            readFiles.append(currentFile)
        n = len(fileset)
        testResults = []
        statsResults = []
        pResults = []
        for i in range(0,n):
            for j in range(i+1, n):
                result = stats.ks_2samp(readFiles[i],readFiles[j])
                pResults.append(result[1])
                statsResults.append(result[0])
                #print(result)
        n_b = 100
        pPlot = fig.add_subplot(4,4,t )
        pPlot.hist(pResults, normed=True, bins=n_b)
        pPlot.set_title('P value for text_id #' + text_id)
        pPlot.set_xlim(0.01,1.1)
        pPlot.set_ylim(0.0,1)
        dPlot = fig.add_subplot(4,4,8 + t)
        dPlot.hist(statsResults, normed=True, bins=n_b)
        dPlot.set_title('D statistical value for text_id #' + text_id)
        dPlot.set_xlim(0.01,1.1)
        
        
   
plt.show()         

#random.seed()
#S = 500
#
#pltIndex = 1
#fig = plt.figure()
#for N in [10,50,100,150,200,250,300,350]:
#    onlyfiles = [f for f in listdir('data/') if isfile(join('data/', f))]
#    print('# files: ' + str(N))
#
#    meanList = []
#    varianceList = []
#    for i in range(0,N):
#        f = len(onlyfiles)
#        j = random.randint(0,f-1)
#        #print('i: ' + str(i) + ' j: ' + str(j))
#        print('# ' + str(i) + ': ' + onlyfiles[j])
#        currentFile = []
#        with open('data/' + onlyfiles[j]) as file:
#            lines = file.readlines()
#            for k in range(0,min(len(lines) - 1,S)):
#                strlen = len(lines)
#                randomLine = random.randint(1,strlen-1)
#                splitLine = lines[randomLine].split()
#                currentFile.append(int(splitLine[11]))
#                lines.pop(randomLine)
#        meanList.append(np.mean(currentFile))
#        varianceList.append(np.var(currentFile))    
#        onlyfiles.pop(j)
#
#    print('Means : ')
#    for mean in meanList:
#        print(str(mean))
#    print('Variance: ')
#    for var in varianceList:
#        print(str(var))
# 
#       
#    n_b = int(int(1+3.3*math.log10(N)))
#    meanPlot = fig.add_subplot(4,4,pltIndex)
#    meanPlot.hist(meanList, normed=True, bins=n_b)
#    meanPlot.set_title('Mean Fixation Duration N = ' + str(N))
#    meanPlot.set_xlim(0,700000)
#    varPlot = fig.add_subplot(4,4, 4 + pltIndex)
#    pltIndex += 1
#    if pltIndex == 5:
#        pltIndex += 4
#    varPlot.hist(varianceList, normed=True, bins = n_b)#bins=int(1.5*n_b) + int(0.001*(n_b*n_b)))
#    varPlot.set_title('Fixation Duration Variance N = ' + str(N))
#    varPlot.set_xlim(0,200000000000)
#    fig.subplots_adjust(left=None, bottom=None, right=None, top=0.95, wspace=None, hspace=0.25)
#    print('_____________')
#
#plt.show()

#csvfile = open('duration.csv', 'r')
#reader = csv.reader(csvfile, delimiter=',', quotechar='|')
#myData = []
#for row in reader:
#    myData.append(int(row[0]))
#myData = sorted(myData)

#m = np.mean(myData)
#std = np.std(myData)
#skewness = stats.skew(myData)
#
#n = len(myData)
#n_b = int(1+3.3*math.log10(n))
#
#
#print('N: ' + str(n) + ' Bins: ' + str(n_b))
#print('Mean : ' + str(m))
#print('Standard diviation : ' + str(std))
#print('Skewness : ' + str(skewness))
##fit = stats.norm.pdf(myData, m, std)
##plt.plot(myData, fit, '-o')
#plt.hist(myData, normed=True, bins=n_b)
#plt.xlim(0, 1500000)
#plt.xlabel('Fixation duration')
#plt.ylabel('Density')
#plt.show()