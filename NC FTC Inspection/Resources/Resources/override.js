function handle() {
	var src = event.target || event.srcElement;
	console.log(src.id);
	if (confirm("Change " + src.id + "?")) {
		var xhttp = new XMLHttpRequest();
		xhttp.onreadystatechange = function() {
			if (this.readyState == 4 && this.status == 204) {
				//refresh the page
				var xhttp1 = new XMLHttpRequest()
				xhttp1.onreadystatechange = function(){
					if (this.readyState == 4 && this.status == 200) {
						document.getElementsByTagName("html")[0].innerHTML=this.responseText;
					}
				}
				xhttp1.open("GET", window.location.href, true);
				xhttp1.send();
			}
		};
		xhttp.open("POST", "override?id="+src.id, true);
		xhttp.send();
	}
}