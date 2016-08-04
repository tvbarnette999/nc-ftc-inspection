const START = 0;
const UP1 = 1;
const UP2 = 2;
const DOWN1 =3;
const DOWN2 = 4;
const LEFT1 = 5;
const RIGHT1 = 6;
const LEFT2 = 7;
const RIGHT2 = 8;
const B = 9;
const KEYUP = 38;
const KEYDOWN = 40;
const KEYLEFT = 37;
const KEYRIGHT = 39;
const KEYA = 65;
const KEYB = 66;
const KAMEN_IMAGE = 'DeanKamen.jpg';
var state = 0;
document.onkeydown = function (e) {
    e = e || window.event;
	key = e.keyCode;
	//begin giant state machine
	switch(state){
		case START:
			if(key == KEYUP){
				state = UP1;
			}
			break;
		case UP1:
			if(key == KEYUP){
				state = UP2;
			} else{
				state = START;
			}
			break;
		case UP2:
			if(key == KEYDOWN){
				state = DOWN1;
			} else{ 
				state = START;
			}
			break;
		case DOWN1:
			if(key == KEYDOWN){
				state = DOWN2;
			}else{
				state = START;
			}
			break;
		case DOWN2:
			if(key == KEYLEFT){
				state = LEFT1;
			} else{ 
				state = START;
			}
			break;
		case LEFT1:
			if(key == KEYRIGHT){
				state = RIGHT1;
			}else{
				state = START;
			}
			break;
		case RIGHT1:
			if(key == KEYLEFT){
				state = LEFT2;
			} else{
				state = START;
			}
			break;
		case LEFT2:
			if(key == KEYRIGHT){
				state = RIGHT2;
			} else{
				state = START;
			}
			break;
		case RIGHT2:
			if(key == KEYB){
				state = B;
			} else{
				state = START;
			}
			break;
		case B:
			if(key == KEYA){
		
				img = new Image();
				img.src = KAMEN_IMAGE;
				img.style.position = 'absolute';
				img.style.left = 400;
				img.style.top = window.innerHeight;
				document.body.appendChild(img);
				var t = setInterval(function(){
					
					//img.style.top = img.style.top - 2;
					var top = img.style.top;
					top = top.substr(0,top.indexOf('px'));
					top = parseInt(top);
					img.style.top = (top-2)+'px';
					
					if(top < window.innerHeight - img.height){
						clearInterval(t);
						setTimeout(function(){
							t = setInterval(function(){
								var top = img.style.top;
								top = top.substr(0,top.indexOf('px'));
								top = parseInt(top);
								img.style.top = (top+2)+'px';
								if(top > window.innerHeight){
									clearInterval(t);
									img.parentNode.removeChild(img);
								}
							}, 10)
						},1000);
					}
				}, 10);
				
			}else{
				state = START;
			}
			break;
		default:
			state = START;
		
		
	}
	
};