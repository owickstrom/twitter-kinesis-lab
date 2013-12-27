function maxCountIn(tags) {
    return _.max(tags, function (tag) { return tag.count; }).count;
}
function partition(items, size) {
    var result = _.groupBy(items, function(item, i) {
        return Math.floor(i/size);
    });
    return _.values(result);
}
function middleSortBy(coll, comparator) {
    if (_.isEmpty(coll)) {
        return [];
    }
    var sorted = _.sortBy(coll, comparator);    
    var high = _.last(sorted);
    var rest = _.initial(sorted);

    // Create two partitions (an array of two arrays) - the first sorted
    // in ascending order, the second in descending order.
    var partitions = _.reduce(partition(rest, 2), function (acc, pair) {
        var left = pair[0] ? [pair[0]] : [];
        var right = pair[1] ? [pair[1]] : [];
        return [acc[0].concat(left), right.concat(acc[1])];
    }, [[], []]);

    // Put the high element in between the partitions.
    partitions.splice(1, 0, [high]);
    // Flatten the partitions with the high element in the middle,
    // resulting in an array with the elements sorted from low to
    // high from both ends.
    return _.flatten(partitions);
}
$(function () {
    var $container = $('.tags-container');

    function addNewTags(cb) {
        $.getJSON('tags', function (tags) {
            var sorted = middleSortBy(tags, 'count');
            var maxCount = maxCountIn(tags);

            // Create a new tags line and style the tags based on count.
            var $tags = $('<div>');
            $tags.addClass('tags');
            var elements = _.each(sorted, function (e) {
                $tag = $('<span>');
                $tag.css('opacity', e.count / maxCount);
                $tag.css('font-size', (e.count / maxCount * 100) + '%');
                $tag.text(e.tag);
                $tags.append($tag);
            });
            $container.append($tags);

            // Center the tags.
            var halfTagsWidth = $tags.outerWidth() / 2;
            $tags.css('margin-left', '-' + halfTagsWidth + 'px');

            cb();
        });
    }

    function refreshTags() {
        var $tags = $('.tags');

        if ($tags.length > 0) {
            $tags.addClass('removed');
            setTimeout(function () {
                $tags.remove();
            }, 2000);
        }
        addNewTags(function () {
            setTimeout(refreshTags, 5000);
        });
    }
    refreshTags();
});
