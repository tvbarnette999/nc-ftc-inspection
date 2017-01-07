function add(){ //TODO fix index number on add and remove
	var team = event.target.parentNode.id;
	event.target.parentNode.parentNode.removeChild(event.target.parentNode);
	var count = document.getElementById("in").childNodes.length - 1;
	var newTr = document.createElement("tr");
	newTr.setAttribute("id", team);
	var ind = document.createElement("td");
	ind.innerHTML = count + 1;
	var teamTd = document.createElement("td");
	teamTd.innerHTML = team;
	
	var upTd = document.createElement("td");
	if(count > 0){
		//if not the first element, add up arrow
		var upButton = document.createElement("button");
		upButton.setAttribute("onclick", "up()");
		upButton.innerHTML = "&uArr;";
		upTd.appendChild(upButton);
		
		//add down arrow to one above it
		var downButton = document.createElement("button");
		downButton.setAttribute("onclick", "down()");
		downButton.innerHTML = "&dArr;"
		var nodes = document.getElementById("in").childNodes;
		nodes[nodes.length - 1].childNodes[3].appendChild(downButton);
		
	}
	var downTd = document.createElement("td");
	var remTd = document.createElement("td");
	var remButton = document.createElement("button");
	remButton.setAttribute("onclick", "remove()");
	remButton.innerHTML = "X";
	remTd.appendChild(remButton);
	document.getElementById("in").appendChild(newTr);
	newTr.appendChild(ind);
	newTr.appendChild(teamTd);
	newTr.appendChild(upTd);
	newTr.appendChild(downTd);
	newTr.appendChild(remTd);
}

function remove(){
	var deadRow = event.target.parentNode.parentNode;
	var team = deadRow.id; //button is in td in tr, tr has id
	
	//if top, remove up arrow of one below
	if(deadRow.previousSibling.previousSibling == null){
		if(deadRow.nextSibling != null){ //if not last
			deadRow.nextSibling.children[2].firstChild.remove();
		}
	}
	var cur = deadRow;
	while(cur.nextSibling != null){
		cur = cur.nextSibling;
		cur.children[0].innerHTML = eval(cur.children[0].innerHTML) - 1;
	}
	//if bottom, remove down of one above
	if(deadRow.nextSibling == null){
		if(deadRow.previousSibling.previousSibling != null){ //if not top
			deadRow.previousSibling.children[3].firstChild.remove();
		}
	}
	deadRow.parentNode.removeChild(deadRow);
	
	var newLi = document.createElement("li");
	newLi.setAttribute("id", team);
	var newBtn = document.createElement("button");
	newBtn.innerHTML = team;
	newBtn.setAttribute("onclick", "add()")
	
	var outDiv = document.getElementById("out");
	var list = outDiv.children;
	var done = false;
	for(var i = 0; i < list.length; i++){
		if(eval(list[i].getAttribute("id")) > eval(team)){
			outDiv.insertBefore(newLi, list[i])
			done = true;
			break;
		}
	}
	if(!done)outDiv.appendChild(newLi);
	newLi.appendChild(newBtn);
	
	
}

//swap the ids and text 
function up(){
	var me = event.target.parentNode.parentNode;
	var above = me.previousSibling;
	var temp = above.id;
	above.id = me.id;
	above.children[1].innerHTML = me.id;
	me.id = temp;
	me.children[1].innerHTML = temp;
}

function down(){
	var me = event.target.parentNode.parentNode;
	var below = me.nextSibling;
	var temp = below.id;
	below.id = me.id;
	below.children[1].innerHTML = me.id;
	me.id = temp;
	me.children[1].innerHTML = temp;
}

function inspect(){
	var url = "/multi_inspect?teams=";
	var rows = document.getElementById("in").childNodes;
	for(var i = 1; i < rows.length; i++){
		url += rows[i].id;
		if(i != rows.length - 1) url += ",";
	}
	console.log(url);
	xhttp.open("GET", url); //todo add comma separated list of teams.
	xhttp.send();
}