require(emov)
require(xlsx)
require(rJava)
require(randomForest)

#demo(fivesec, package="emov")
#print(fivesec$time)
#print(fivesec$x)
#print(fivesec$y)
#
##fivesec$x = filter(fivesec$x, rep(1/3, 3))
##fivesec$y = filter(fivesec$y, rep(1/3, 3))
#
#fixations = emov.idt(fivesec$time, fivesec$x, fivesec$y, 2, 20)
#
#print(fixations)

t = (0:99)
x = (0.0:99.0)
y = (0.0:99.0)
#print(t)
#print(x)
#print(y)
fixations = emov.idt(t, x, y, 3, 1)
print(fixations)

participant_ratings <- read.xlsx("participant_ratings.xlsx", sheetName="user ratings")
participant_ratings <- participant_ratings[,0:6] #Drop not-used columns
print(participant_ratings)

feature_parameters <- read.xlsx("feature_parameters.xlsx", sheetName="user ratings")
feature_parameters <- feature_parameters[,0:6] #Drop not-used columns
print(feature_parameters)
