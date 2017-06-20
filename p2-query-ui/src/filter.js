// adapted from https://github.com/alexcurtis/react-treebeard/blob/master/example/styles.js
// for multiple source roots
export const defaultMatcher = (filterText, node) => {
  if (node.name) {
    return node.name.toLowerCase().indexOf(filterText.toLowerCase()) !== -1;
  } else {
    return false;
  }
};

export const findNode = (node, filter, matcher) => {
  return matcher(filter, node) || // i match
    (node.children && // or i have decendents and one of them match
      node.children.length &&
      !!node.children.find(child => findNode(child, filter, matcher)));
};

export const filterTree = (node, filter, matcher = defaultMatcher) => {
  if (Array.isArray(node)) {
    // We got the root element here
    const filtered = node
      .filter(child => findNode(child, filter, matcher))
      .map(child => filterTree(child, filter, matcher));
    return filtered;
  } else {
    // If im an exact match then all my children get to stay
    if (matcher(filter, node) || !node.children) { return node; }
    // If not then only keep the ones that match or have matching descendants
    const filtered = node.children
      .filter(child => findNode(child, filter, matcher))
      .map(child => filterTree(child, filter, matcher));
    return Object.assign({}, node, { children: filtered });
  }
};

export const expandFilteredNodesRoot = (node, filter, matcher = defaultMatcher) => {
  return node.map(child => expandFilteredNodes(child, filter, matcher))
}

export const expandFilteredNodes = (node, filter, matcher = defaultMatcher) => {
  let children = node.children;
  if (!children || children.length === 0) {
    return Object.assign({}, node, { toggled: false });
  }
  const childrenWithMatches = node.children.filter(child => findNode(child, filter, matcher));
  const shouldExpand = childrenWithMatches.length > 0;
  // If im going to expand, go through all the matches and see if thier children need to expand
  if (shouldExpand) {
    children = childrenWithMatches.map(child => {
      return expandFilteredNodes(child, filter, matcher);
    });
  }
  return Object.assign({}, node, {
    children: children,
    toggled: shouldExpand
  });
};