/* eslint-disable no-undef */
import httpUtil from './httpUtil';
/**
 * 全部书签数据key
 */
export const TREE_LIST_KEY = 'treeListData';
/**
 * 当前用户书签版本
 */
export const TREE_LIST_VERSION_KEY = 'treeDataVersion';

/**
 * 缓存书签数据
 */
export async function cacheBookmarkData() {
  let key = getCacheKey();
  let res = await localforage.getItem(key);
  //如果没有缓存
  if (!res) {
    res = await httpUtil.get('/bookmark/currentUser');
    if (!res['']) {
      res[''] = [];
    }
    let version = (await httpUtil.get('/user/currentUserInfo')).version;
    await localforage.setItem(key, res);
    await localforage.setItem(TREE_LIST_VERSION_KEY, version);
  }
  window[TREE_LIST_KEY] = res;
}

/**
 * 获取缓存数据
 * @param {*} path path
 */
export function getBookmarkList(path) {
  if (window[TREE_LIST_KEY][path] === undefined) {
    window[TREE_LIST_KEY][path] = [];
  }
  return window[TREE_LIST_KEY][path];
}

/**
 * 检查缓存情况
 * @return  返回true说明未过期，否则说明过期了
 */
export async function checkCacheStatus() {
  let version = await localforage.getItem(TREE_LIST_VERSION_KEY);
  let realVersion = (await httpUtil.get('/user/currentUserInfo')).version;
  return version >= realVersion;
}

/**
 * 清楚缓存数据
 */
export async function clearCache() {
  await localforage.removeItem(getCacheKey());
  await localforage.removeItem(TREE_LIST_VERSION_KEY);
}

/**
 * 更新本地缓存
 */
export async function updateCurrentChangeTime() {
  let version = await localforage.getItem(TREE_LIST_VERSION_KEY);
  await localforage.setItem(TREE_LIST_VERSION_KEY, version + 1);
  await localforage.setItem(getCacheKey(), window[TREE_LIST_KEY]);
}

/**
 * 新增一个节点数据
 * @param {*} currentNode
 * @param {*} node
 */
export async function addNode(currentNode, node) {
  debugger;
  let treeDataMap = window[TREE_LIST_KEY];
  if (currentNode) {
    let key = currentNode.path + '.' + currentNode.bookmarkId;
    if (!treeDataMap[key]) {
      treeDataMap[key] = [];
    }
    treeDataMap[key].push(node);
  } else {
    treeDataMap[''].push(node);
  }
  await updateCurrentChangeTime();
}

/**
 * 批量删除节点
 * @param {*} folderIdList  删除的文件夹id
 * @param {*} bookmarkIdList 删除的书签id
 */
export async function deleteNodes(nodeList) {
  let data = window[TREE_LIST_KEY];
  nodeList.forEach(item => {
    let list = data[item.path];
    let index = list.findIndex(one => one.bookmarkId === item.bookmarkId);
    if (index > -1) {
      list.splice(index, 1);
    }
    //如果是文件夹还是把他的子节点删除
    if (item.type === 1) {
      let key = item.path + '.' + item.bookmarkId;
      Object.keys(data).forEach(one => {
        if (one.startsWith(key)) {
          delete data[one];
        }
      });
    }
  });
  await updateCurrentChangeTime();
}

/**
 * 节点拖拽方法
 * @param {*} info
 */
export async function moveNode(info) {
  debugger;
  let data = window[TREE_LIST_KEY];
  const target = info.node.props.dataRef;
  const current = info.dragNode.props.dataRef;
  //从原来位置中删除当前节点
  let currentList = data[current.path];
  currentList.splice(
    currentList.findIndex(item => item.bookmarkId === current.bookmarkId),
    1
  );
  //请求体
  const body = {
    bookmarkId: current.bookmarkId,
    sourcePath: current.path,
    targetPath: '',
    //-1 表示排在最后
    sort: -1
  };
  if (info.dropToGap) {
    body.targetPath = target.path;
    //移动到目标节点的上面或者下面
    let targetList = data[target.path];
    //目标节点index
    let index = targetList.indexOf(target);
    //移动节点相对于目标节点位置的增量
    let addIndex = info.dropPosition > index ? 1 : 0;
    body.sort = target.sort + addIndex;
    targetList.splice(index + addIndex, 0, current);
    for (let i = index + 1; i < targetList.length; i++) {
      targetList[i].sort += 1;
    }
  } else {
    //移动到一个文件夹下面
    body.targetPath = target.path + '.' + target.bookmarkId;
    let targetList = data[body.targetPath];
    if (!targetList) {
      targetList = [];
      data[body.targetPath] = targetList;
    }
    body.sort = targetList.length > 0 ? targetList[targetList.length - 1].sort + 1 : 1;
    targetList.push(current);
  }
  //更新节点的path和对应子节点path
  current.path = body.targetPath;
  current.sort = body.sort;
  //如果为文件夹还要更新所有子书签的path
  if (body.sourcePath !== body.targetPath) {
    let keys = Object.keys(data);
    //旧路径
    let oldPath = body.sourcePath + '.' + current.bookmarkId;
    //新路径
    let newPath = body.targetPath + '.' + current.bookmarkId;
    keys.forEach(item => {
      if (!item.startsWith(oldPath)) {
        return;
      }
      let newPathStr = item.replace(oldPath, newPath);
      let list = data[item];
      delete data[item];
      data[newPathStr] = list;
      list.forEach(item1 => (item1.path = newPathStr));
    });
  }
  await updateCurrentChangeTime();
  return body;
}

/**
 * 关键词搜索方法
 * @param {*} content
 */
export async function keySearch(content) {
  let time1 = Date.now();
  content = content.toLocaleLowerCase().trim();
  let res = [];
  let arrs = Object.values(window[TREE_LIST_KEY]);
  for (let i1 = 0, length1 = arrs.length; i1 < length1; i1++) {
    for (let i2 = 0, length2 = arrs[i1].length; i2 < length2; i2++) {
      let item = arrs[i1][i2];
      if (item.type === 1) {
        continue;
      }
      if (item.searchKey.indexOf(content) > -1) {
        res.push(item);
        if (res.length >= 12) {
          console.info('搜索耗时：' + (Date.now() - time1));
          return res;
        }
      }
    }
  }
  console.info('搜索耗时：' + (Date.now() - time1));
  return res;
}

/**
 * 获取localstore缓存的key
 */
export function getCacheKey() {
  let currentId = JSON.parse(window.atob(window.token.split('.')[1])).userId;
  return currentId + TREE_LIST_KEY;
}
