package acr.browser.lightning.adblock.allowlist

import acr.browser.lightning.database.allowlist.AdBlockAllowListRepository
import acr.browser.lightning.database.allowlist.AllowListEntry

class FakeAdBlockAllowListRepository(
    var allowList: MutableList<AllowListEntry> = mutableListOf()
) : AdBlockAllowListRepository {
    override suspend fun allAllowListItems(): List<AllowListEntry> = allowList

    override suspend fun allowListItemForUrl(url: String): AllowListEntry? =
        allowList.firstOrNull { (domain) -> url == domain }

    override suspend fun addAllowListItem(whitelistItem: AllowListEntry) {
        allowList.add(whitelistItem)
    }

    override suspend fun removeAllowListItem(whitelistItem: AllowListEntry) {
        allowList.remove(whitelistItem)
    }

    override suspend fun clearAllowList() {
        allowList.clear()
    }
}
