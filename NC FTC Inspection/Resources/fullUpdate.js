function update(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../fullupdate?team="+event.target.name+"&value="+event.target.checked, true);
	xhttp.send();
}

function fullpass(){
	
	
	sendNote();
	//check everything is passed first. If not, popup and do not post.
	var inputs=document.getElementsByTagName("input");
	var length = inputs.length;
	var allPass=true;
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../cubeindex?",false);
	xhttp.send();
	var cubeIndex=parseInt(xhttp.responseText);
	//TODO ask for cube index 
	
	for (var i = 0; i < length; i++) {
		if(i==cubeIndex)continue;
	    if(inputs[i].type=="checkbox" && !inputs[i].checked){
	    	allPass=false;
	    	break;
	    }
	}
	if(allPass){
	
		//TODO get signatures.
		var teamsig = prompt("I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of the FIRST Tech Challenge have been abided by.\nTeam Student Representative:");
		if(teamsig == null || teamsig==""){
			return;
		}
		
		var inspectorsig = prompt("I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of the FIRST Tech Challenge have been abided by.\nInspector:");
		if(inspectorsig == null || inspectorsig==""){
			return;
		}
		var xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../sig?team="+event.target.name, true);
		xhttp.send("XXteam=".concat(teamsig.concat("&inspector=".concat(inspectorsig.concat("&&&")))));
		
		xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../update?team="+event.target.name+"&value=3", true);
		xhttp.send();
	}else{
		window.alert("Not all items have passed!");
	}
}

function fullfail(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../update?team="+event.target.name+"&value=1", true);
	xhttp.send();
	sendNote();
}

function sendNote(){
	var n=document.getElementById("note");
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../note?team="+n.name, true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("XX".concat(n.value.concat("&&&")));
}