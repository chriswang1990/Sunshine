# Sunshine
Android Weather App - Udacity Android course project 
Several significant improvements to the original course project:
1. Call the Google timezone API to get the timezone ID for the query location, fix the bug that the app is not displaying local date
2. Use Date class instead of the deprecated Time class for date, simplify the code and more robust in date calculation
3. The today entry is auto selected when the app startup in two-panel mode, after changing location, the entry in the some position will be auto selected
4. Use the new toolbar Class instead of the deprecated ActionBar class, separate the activity and content XML accordingly 
