//default tooltip style

$.fn.qtip.styles.wmstyle = { // Last part is the name of the style
   color: 'white',
   background: '#373737' ,
   fontSize: '14px',
   border: {
   	 width: 5,
     radius: 10,
	 color: '#373737'
   },
   padding: 3 , 
   textAlign: 'left',
   tip: true, 
   name: 'cream'
}

//gathering url params

var urlParams = {};

(function () {
    var e,
        a = /\+/g,  // Regex for replacing addition symbol with a space
        r = /([^&=]+)=?([^&]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.search.substring(1);

    while (e = r.exec(q))
       urlParams[d(e[1])] = d(e[2]);
})();


function normalize(val, min, max) {
	
	if (min == max) return 1 ;
	
	return (val-min) * (1/(max-min)) ;
}


