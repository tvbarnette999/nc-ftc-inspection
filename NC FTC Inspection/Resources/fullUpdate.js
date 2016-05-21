function update(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "fullupdate?team="+event.target.name+"&value="+event.target.checked, true);
	xhttp.send();
	document.cookie = "username=FTC_VERIFIED";
}