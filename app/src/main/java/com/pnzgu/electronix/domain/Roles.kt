package com.pnzgu.electronix.domain

import com.pnzgu.electronix.data.dto.UserDto

fun UserDto?.hasRole(code: String): Boolean =
    this?.roles?.any { it.equals(code, ignoreCase = true) } == true

fun UserDto?.isAdministrator(): Boolean = hasRole("ADMINISTRATOR")
fun UserDto?.isManager(): Boolean = hasRole("MANAGER") || isAdministrator()
fun UserDto?.isModerator(): Boolean = hasRole("MODERATOR") || isAdministrator()

/** Manager, moderator, or administrator (shown staff UI such as roles on profile). */
fun UserDto?.isStoreStaff(): Boolean =
    hasRole("MANAGER") || hasRole("MODERATOR") || hasRole("ADMINISTRATOR")
