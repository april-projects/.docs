require(["gitbook","jQuery"],function(o,t){var s,n,e,i;window.history.pushState&&window.sessionStorage&&(s="book_summary_scroll_postion_saver",e=function(){window.sessionStorage.setItem(s,n.scrollTop())},i=window.history.pushState,window.history.pushState=function(o){return e(),i.apply(window.history,arguments)},o.events.bind("page.change",function(){var o=Number(window.sessionStorage.getItem(s),10)||0;n=t(".book-summary .summary"),window.setTimeout(function(){n.scrollTop(o)},50)}))});