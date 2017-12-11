/**
 * Equal Heights jQuery plugin
 * Equalize the heights of elements. For columns or any elements that need to be the same size (floats, etc).
 * 
 * Usage: $(object).equalHeights([minHeight], [maxHeight]);
 * 
 * Example 1: $(".cols").equalHeights(); Sets all columns to the same height.
 * Example 2: $(".cols").equalHeights(400); Sets all cols to at least 400px tall.
 * Example 3: $(".cols").equalHeights(100,300); Cols are at least 100 but no more
 * than 300 pixels tall. Elements with too much content will gain a scrollbar.
 * 
 */
(function($) {
	$.fn.equalHeights = function(minHeight, maxHeight) {
		var tallest = (minHeight) ? minHeight : 0;
		this.each(function() {
			$(this).css('height','auto');
			if($(this).height() > tallest) {
				tallest = $(this).outerHeight();
			}
		});
		if((maxHeight) && tallest > maxHeight) tallest = maxHeight;
		return this.each(function() {
			//$(this).height(tallest).css("overflow","auto");
			$(this).css('height',tallest);
		});
	}
})(jQuery);