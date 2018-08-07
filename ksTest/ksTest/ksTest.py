import numpy as np
import scipy.stats as stats
import matplotlib.pyplot as plt
import csv
import math
import random
import re
import sys
from os import listdir
from os.path import isfile, join

data_folder = 'data/'
allfiles = [f for f in listdir(data_folder) if isfile(join(data_folder, f))]
alpha = 0.05
fig = plt.figure()

mode =  input('Select test mode: \'users\' for user, \'texts\' for text\t') #u = USER t = TEXT
outputMode = input('Select test mode: \'p\' for p values, \'d\' for d statistic values \t') #0 = P, 1 = D
if outputMode != 'p' and outputMode != 'd':
    print('Invalid mode, please use \'p\' or \'d\' as input next time (:')
    mode = 'leave'
#PER USER
if mode == 'users':
    average_p_file = open("ks_out/" + mode +"/overall_results.csv","w")
    average_p_file.write("user_id, portion of pairs with p < alpha" + str(alpha) + "\n")
    for f in range(1,36):
        n_true = 0
        n_false = 0

        fileset = []
        print('Evaluating user id#'+str(f))
        user_id = str(f)
        if len(user_id) == 1:
            user_id = '0' + user_id
        user_id_re = 'p'+user_id+'+\w*'
        for file in allfiles:
            if re.match(user_id_re,file):                                       #use regular expression to find relevant files
                fileset.append(data_folder + file)
        readFiles = []
        textIds = []
        for fileName in fileset:
            #print(fileName)
            currentFile = []
            with open(fileName, 'r') as file:
                next(file)
                for line in file:
                    splitLine = line.split()
                    currentValue = int(splitLine[11])
                    #if(currentFile.count != [-1]):
                    if(len(currentFile) > 0 and currentFile[len(currentFile) - 1] == currentValue):
                        continue;
                    currentFile.append(currentValue)
            readFiles.append(currentFile)
            splitFileName = fileName.split('_')
            textIds.append(splitFileName[1])
        n = len(fileset)
        if n == 0:                                                              #if user has no entries - proceed with the next user_id
            continue

        csvFile =  open('ks_out/'+mode+'/' + outputMode + '/ks_' + outputMode + '_user'+user_id+'.csv','w')
        testResults = []
        statsResults = []
        pResults = []
        csvFile.write('text_id')
        for i in range(0,n):
            csvFile.write(', ' + textIds[i])
        csvFile.write('\n')
        for i in range(0,n):
            csvFile.write(textIds[i])
            for j in range(0, n):
                result = stats.ks_2samp(readFiles[i],readFiles[j])              #perform ks test
                pResults.append(result[1])                                      #save the result
                statsResults.append(result[0])
                if outputMode == 'p':                                           #|\
                    csvFile.write(', ' + str(round(result[1],9)))               #|= Output the p or d value
                elif outputMode == 'd':                                         #|= depending on the mode
                    csvFile.write(', ' + str(round(result[0],9)))               #|/
                if outputMode == 'p':
                    if result[1] < 0.05:
                        n_true += 1
                    else:
                        n_false += 1
                #print(result)
            csvFile.write('\n')
        average_p_file.write(str(f) + "," + str(round(n_true/(n_true+n_false),9))+"\n")
       # n_b = 100                                                             #|\
       #pPlot = fig.add_subplot(4,4,f)                                         #|\\
       #pPlot.hist(pResults, normed=True, bins=n_b)                            #|\\\
       #pPlot.set_title('P value for user_id #' + str(f))                      #|\\\\
       #pPlot.set_xlim(0.01,1.1)                                               #||||||this code was used to plot
       #pPlot.set_ylim(0.0,1)                                                  #||||||p and d values for each user
       #dPlot = fig.add_subplot(4,4,8 + f)                                     #|////
       #dPlot.hist(statsResults, normed=True, bins=n_b)                        #|///
       #dPlot.set_title('D statistical value for user_id #' + str(f))          #|//
       #dPlot.set_xlim(0.01,1.1)                                               #|/


#PER TEXT
elif mode == 'texts':
    text_ids = [11299,13165,5938,11143,322,10879,4584,4504,6366,4701,6474,4094,7784,6046,2725,3775,9977,3819]
    T = len(text_ids)
    average_p_file = open("ks_out/" + mode +"/overall_results.csv","w")
    average_p_file.write("text_id, portion of pairs with p < alpha" + str(alpha) + "\n")
    for t in range(1, len(text_ids) + 1):
        n_true = 0
        n_false = 0

        fileset = []
        text_id = str(text_ids[t-1])
        print('Evaluating text id#'+str(text_ids[t-1]))
        csvFile =  open('ks_out/'+mode+'/' + outputMode + '/ks_' + outputMode + '_text'+str(text_ids[t-1])+'.csv','w')
        text_id_re = '\w*'+str(text_id)+'+\w*'
        for file in allfiles:
            if re.match(text_id_re,file):                                       #use regular expression to find relevant files
                fileset.append(data_folder + file)
        readFiles = []
        userIds = []
        for fileName in fileset:
            #print(fileName)
            currentFile = []
            with open(fileName, 'r') as file:
                next(file)
                for line in file:
                    splitLine = line.split()
                    currentFile.append(int(splitLine[11]))
            readFiles.append(currentFile)
            splitFileName = fileName.split('_')
            userIds.append(splitFileName[0][-2:])
        n = len(fileset)
        csvFile.write('user_id')
        for i in range(0,n):
            csvFile.write(', ' + userIds[i])
        csvFile.write('\n')
        testResults = []
        statsResults = []
        pResults = []
        for i in range(0,n):
            csvFile.write(userIds[i])
            for j in range(0, n):
                result = stats.ks_2samp(readFiles[i],readFiles[j])              #perform ks test
                pResults.append(result[1])                                      #save the result
                statsResults.append(result[0])                                  
                if outputMode == 'p':                                           #|\ 
                    csvFile.write(', ' + str(round(result[1],9)))               #|= Output the p or d value 
                elif outputMode == 'd':                                           #|= depending on the mode 
                    csvFile.write(', ' + str(round(result[0],9)))               #|/ 
                if outputMode == 'p':
                    if result[1] < 0.05:
                        n_true += 1
                    else:
                        n_false += 1
                #print(result)
            csvFile.write('\n')
        n_b = 100
        average_p_file.write(str(t) + "," + str(round(n_true/(n_true+n_false),9))+"\n")
        #pPlot = fig.add_subplot(4,4,t )                                        #|\
        #pPlot.hist(pResults, normed=True, bins=n_b)                            #|\\
        #pPlot.set_title('P value for text_id #' + text_id)                     #|\\\
        #pPlot.set_xlim(0.01,1.1)                                               #|\\\\
        #pPlot.set_ylim(0.0,1)                                                  #||||||this code was used to plot p and d values for each text
        #dPlot = fig.add_subplot(4,4,8 + t)                                     #|////
        #dPlot.hist(statsResults, normed=True, bins=n_b)                        #|///
        #dPlot.set_title('D statistical value for text_id #' + text_id)         #|//
        #dPlot.set_xlim(0.01,1.1)                                               #|/
elif mode != 'leave':                                                              
    print('Invalid mode, please use \'users\' or \'texts\' as input next time (:')  
    sys.exit(0)   
    
average_p_file.close()