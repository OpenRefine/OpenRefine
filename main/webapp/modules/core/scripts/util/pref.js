/*
This utility is used for storing user preferences to enhance user experience.
Inspiration: https://github.com/OpenRefine/OpenRefine/issues/2316

Further to fulfilling the above requirement, this utility can be extended to:
1. Storing any user preference which saves rework from user's side,
   Example: Page size for showing number or records of a table
2. If the preference is to be tightly bound to a user,
   they can be passed to and fetched from the backend too

Currently, cookies are being used for preference storage, and individual
preference is stored separately. However, if we plan on growing the number
of preferences, we can use browser cache or backend, since cookie parsing
can get more expensive.
*/

function setPreference(key, value) {
	//console.log("will set pref, " + key + "-" + value);
	document.cookie = key + "=" + value + ";path=/";
}

function getPreference(key) {
	//console.log("will get pref, " + key);
	var value = getCookie(key);
	// console.log("got pref, " + value);
	return value && value == "true";
}

function getCookie(key) {
	var name = key + "=";
	var decodedCookie = decodeURIComponent(document.cookie);
	var ca = decodedCookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ') {
			c = c.substring(1);
		}
		if (c.indexOf(name) == 0) {
			return c.substring(name.length, c.length);
		}
	}
	return "";
}