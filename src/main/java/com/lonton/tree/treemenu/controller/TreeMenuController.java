package com.lonton.tree.treemenu.controller;

import com.lonton.tree.treemenu.common.util.Result;
import com.lonton.tree.treemenu.pojo.entity.TreeMenu;
import com.lonton.tree.treemenu.mapper.TreeMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (TreeMenu)表控制层
 * <p/>
 * @author 张利红
 */
@RestController
@RequestMapping("treeMenu")
public class TreeMenuController {

    @Autowired
    TreeMenuMapper treeMenuMapper;

    /**
     * 获取根节点菜单 <br/>
     * @return  根节点菜单
     */
    @GetMapping("")
    public Result getRootMenus() {
        List<TreeMenu> menus = treeMenuMapper.getRootMenus();
        for (TreeMenu menu : menus) {
            Boolean leaf = isLeaf(menu.getMenuId());
            menu.setIsLeaf(leaf);
        }
        return Result.ok().data("menus", menus);
    }

    /**
     * 根据父节点查询子节点数据 <br/>
     * @param menuId <br/>
     * @return 子节点
     */
    @GetMapping("/{menuId}")
    public Result getChildren(@PathVariable Long menuId){
         // 根据父 menuId 查询所有的子 menu
        List<TreeMenu> menus = treeMenuMapper.getChildren(menuId);
        for (TreeMenu menu : menus) {
            Boolean leaf =isLeaf(menu.getMenuId());
            menu.setIsLeaf(leaf);
        }
        return Result.ok().data("menus", menus);
    }

    /**
     * 根据名称查询 <br/>
     * @param menuName <br/>
     * @return 菜单树
     */
    @GetMapping("/search/{menuName}")
    public Result searchMenus(@PathVariable String menuName){
        List<TreeMenu> menus = searchItems(menuName);
        return Result.ok().data("menus", menus);
    }

    /**
     * 判断是否是叶子节点 <br/>
     * @param menuId <br/>
     * @return 是否
     */
    public Boolean isLeaf(Long menuId) {
        List<TreeMenu> children =treeMenuMapper.getChildren(menuId);
        if (CollectionUtils.isEmpty(children)) {
            return true;
        }
        return false;
    }
    /**
     * 搜素查询 <br/>
     * 相当于模糊查询。与之前在xml里实现的模糊查询相比，
     * 不单单查的是包含关键字的字段，它能将包含关键字的
     * 父节点也返回，能够以一棵树的形式返回
     * @param menuName <br/>
     * @return 菜单树
     */
  public List<TreeMenu> searchItems(String menuName) {
        // 先查询出所有的数据
        List<TreeMenu> allMenus = treeMenuMapper.getAllTreeMenu();
        Map<Long, TreeMenu> menuMap = new HashMap<>();
        for (TreeMenu menu : allMenus) {
            menuMap.put(menu.getMenuId(), menu);
        }

        // 再在查询出所有的数据中进行搜索过滤
        List<TreeMenu> menusAfterSearched = new ArrayList<>();
        for (TreeMenu menu : allMenus) {
            if (menu.getMenuName().contains(menuName)) {
                if (!menusAfterSearched.contains(menu)) {
                    menusAfterSearched.add(menu);
                }
                // 父节点
                Long parentId = menu.getParentMenuId();
                while (parentId != 0) {
                    TreeMenu treeMenu = menuMap.get(parentId);

                    if (!menusAfterSearched.contains(treeMenu)) {
                        menusAfterSearched.add(treeMenu);
                    }
                    parentId = treeMenu.getParentMenuId();
                }
            }
        }
        return buildTree(menusAfterSearched);
    }
    /**
     * 构建树形结构 <br/>
     * 参照 Cascader 级联选择器 <br/>
     * @param list
     */
    public static List<TreeMenu> buildTree(List<TreeMenu> list){
        // 获取parentId = 0 的节点
        List<TreeMenu> root = new ArrayList<>();
        for (TreeMenu menu : list) {
            if (menu.getParentMenuId() == 0L){
                root.add(menu);
            }
        }
        // 根据 parentMenuId 进行分组
        HashMap<Long, List<TreeMenu>> map = new HashMap<>();
        for (TreeMenu menu : list) {
            if(map.containsKey(menu.getParentMenuId())){
                map.get(menu.getParentMenuId()).add(menu);
            }else{
                List<TreeMenu> menus = new ArrayList<>();
                menus.add(menu);
                map.put(menu.getParentMenuId(), menus);
            }
        }
        // 递归遍历节点
        recursionFnTree(list, map);
        return root;
    }

    /**
     * 递归遍历节点 <br/>
     * @param list <br/>
     * @param map 节点叔祖
     */
    public static void recursionFnTree(List<TreeMenu> list, Map<Long,List<TreeMenu>> map) {
        for (TreeMenu menu : list) {
            List<TreeMenu> childList = map.get(menu.getMenuId());
            menu.setChildren(childList);
            if (null != childList && 0 < childList.size()) {
                recursionFnTree(childList, map);
            }
        }
    }
}

