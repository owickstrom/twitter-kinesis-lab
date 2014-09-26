/** @jsx React.DOM */

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

var Tags = React.createClass({
  name: 'Tags',
  getInitialState: function () {
    return {
      tags: []
    };
  },
  getDefaultProps: function () {
    return {
      updateInterval: 15000
    };
  },
  componentDidMount: function () {
    this.updateTags();
  },
  updateTags: function () {
    request({ json: true, url: '/tags' }, function (err, response, body) {

      // Set the new tags.
      this.setState({
        tags: middleSortBy(body, 'count')
      });
      // Update tags in another N seconds.
      setTimeout(this.updateTags, this.props.updateInterval);

    }.bind(this));
  },
  render: function () {
    var tags = this.state.tags || [];
    var maxCount = maxCountIn(tags);

    var tagNodes = tags.map(function (tag) {
      var size = tag.count / maxCount * 400;
      var opacity = (tag.count / maxCount + 0.5) /2;

      var style = {
        fontSize: size + '%',
        opacity: opacity
      };
      var key = tag.tag;

      var href = 'https://twitter.com/search?q=' + encodeURIComponent(tag.tag);
      return (
        <li style={style} key={key}>
          <a href={href} target="_blank">
            #{tag.tag}
          </a>
        </li>
      );
    });

    return (
      <ul className="tags">
        { tagNodes }
      </ul>
    );
  }
});

var Page = React.createClass({
  name: 'Page',
  render: function () {
    return (
      <div>
        <h1>Popular Twitter Tags</h1>
        <Tags />
      </div>
    );
  }
});

React.renderComponent(
  <Page />,
  document.getElementById('container')
);
