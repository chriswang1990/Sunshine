# Sunshine
#### Android Weather App - Udacity Android course project 
##### Several significant improvements to the original course project:
* Call the Google timezone API to get the timezone ID for the query location, fix the bug that the app is not displaying correct local date and weather for the query location
* Use Date class instead of the deprecated Time class for date, simplify the code and make date calculation more robust
* (In in two-panel mode) The today entry is auto selected when the app startup. After changing location, the entry in the same position will also be auto selected
* Use the new toolbar Class instead of the deprecated ActionBar class, separate the activity and content layout XML accordingly 
* Display city name in today list item view and detail fragment to remind user
