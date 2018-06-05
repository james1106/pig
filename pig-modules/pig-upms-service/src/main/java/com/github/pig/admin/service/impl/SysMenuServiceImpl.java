/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.github.pig.admin.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.github.pig.admin.common.util.TreeUtil;
import com.github.pig.admin.mapper.SysMenuMapper;
import com.github.pig.admin.model.dto.MenuTree;
import com.github.pig.admin.model.entity.SysMenu;
import com.github.pig.admin.service.SysMenuService;
import com.github.pig.common.constant.CommonConstant;
import com.github.pig.common.util.Assert;
import com.github.pig.common.vo.MenuVO;
import com.xiaoleilu.hutool.collection.CollUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>
 * 菜单权限表 服务实现类
 * </p>
 *
 * @author lengleng
 * @since 2017-10-29
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Override
    @Cacheable(value = "menu_details", key = "#role  + '_menu'")
    public List<MenuVO> findMenuByRoleName(String role) {
        return sysMenuMapper.findMenuByRoleName(role);
    }

    @Override
    public String[] findPermission(String[] roles) {
        Set<MenuVO> menuVoSet = new HashSet<>();
        for (String role : roles) {
            List<MenuVO> menuVos = findMenuByRoleName(role);
            menuVoSet.addAll(menuVos);
        }

        Set<String> permissions = new HashSet<>();
        for (MenuVO menuVo : menuVoSet) {
            if (StringUtils.isNotEmpty(menuVo.getPermission())) {
                String permission = menuVo.getPermission();
                permissions.add(permission);
            }
        }

        return permissions.toArray(new String[permissions.size()]);
    }

    @Override
    @CacheEvict(value = "menu_details", allEntries = true)
    public Boolean deleteMenu(Integer id) {
        Assert.isNull(id, "菜单ID不能为空");
        // 删除当前节点
        SysMenu condition1 = new SysMenu();
        condition1.setMenuId(id);
        condition1.setDelFlag(CommonConstant.STATUS_DEL);
        this.updateById(condition1);

        // 删除父节点为当前节点的节点
        SysMenu conditon2 = new SysMenu();
        conditon2.setParentId(id);
        SysMenu sysMenu = new SysMenu();
        sysMenu.setDelFlag(CommonConstant.STATUS_DEL);
        return this.update(sysMenu, new EntityWrapper<>(conditon2));
    }

    @Override
    @CacheEvict(value = "menu_details", allEntries = true)
    public Boolean updateMenuById(SysMenu sysMenu) {
        return this.updateById(sysMenu);
    }

    /**
     * 返回角色的菜单
     *
     * @param roleNames 角色
     * @return 菜单列表
     */
    @Override
    public List<MenuTree> findUserMenuTree(List<String> roleNames) {
        // 获取符合条件得菜单
        Set<MenuVO> all = new HashSet<>();
        roleNames.forEach(roleName -> all.addAll(findMenuByRoleName(roleName)));
        List<MenuTree> menuTreeList = new ArrayList<>();
        all.forEach(menuVo -> {
            if (CommonConstant.MENU.equals(menuVo.getType())) {
                menuTreeList.add(new MenuTree(menuVo));
            }
        });
        CollUtil.sort(menuTreeList, Comparator.comparingInt(MenuTree::getSort));
        return TreeUtil.bulid(menuTreeList, -1);
    }

    /**
     * 返回多个角色的菜单
     *
     * @param roleNames 角色列表
     * @return 菜单列表
     */
    @Override
    public List<MenuVO> findMenuByRoles(List<String> roleNames) {
        List<MenuVO> all = new ArrayList<>();
        roleNames.forEach(roleName -> all.addAll(findMenuByRoleName(roleName)));
        return all;
    }
}
