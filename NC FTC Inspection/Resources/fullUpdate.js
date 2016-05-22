function update(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../fullupdate?team="+event.target.name+"&value="+event.target.checked, true);
	xhttp.send();
	document.cookie = "username=FTC_VERIFIED";
}

function fullpass(){
	//check everything is passed first.
	//get signitures
	//TODO send note
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../update?team="+event.target.name+"&value=3", true);
	xhttp.send();
	document.cookie = "username=FTC_VERIFIED";
}

function fullfail(){
	//TODO send note
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../update?team="+event.target.name+"&value=1", true);
	xhttp.send();
	document.cookie = "username=FTC_VERIFIED";
}