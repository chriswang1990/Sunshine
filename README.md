# Sunshine
#### Android Weather App - Udacity Android course project 
##### Several significant improvements to the original course project:
* Call the Google timezone API to get the timezone ID for the query location, fix the bug that the app is not displaying local date
* Use Date class instead of the deprecated Time class for date, simplify the code and more make date calculation more robust
* (In in two-panel mode) The today entry is auto selected now when the app startup. After changing location, the entry in the same position will be auto selected
* Use the new toolbar Class instead of the deprecated ActionBar class, separate the activity and content layout XML accordingly 
