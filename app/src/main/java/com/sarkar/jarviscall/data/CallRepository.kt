package com.sarkar.jarviscall.data

import kotlinx.coroutines.flow.Flow

class CallRepository(private val db: AppDatabase) {

    val allCallLogs: Flow<List<CallLogEntity>> = db.callLogDao().getAllCallLogs()
    val allRules: Flow<List<RuleEntity>> = db.ruleDao().getAllRules()
    val allContactFilters: Flow<List<ContactFilterEntity>> = db.contactFilterDao().getAllContactFilters()

    suspend fun addCallLog(log: CallLogEntity) = db.callLogDao().insertCallLog(log)
    suspend fun deleteCallLog(id: Long) = db.callLogDao().deleteCallLogById(id)
    suspend fun clearAllCallLogs() = db.callLogDao().clearAllLogs()

    suspend fun addRule(rule: RuleEntity) = db.ruleDao().insertRule(rule)
    suspend fun deleteRule(id: Long) = db.ruleDao().deleteRuleById(id)

    suspend fun addContactFilter(filter: ContactFilterEntity) = db.contactFilterDao().insertFilter(filter)
    suspend fun deleteContactFilter(id: Long) = db.contactFilterDao().deleteFilterById(id)

    suspend fun populateDefaultRulesIfNeeded() {
        val activeRules = db.ruleDao().getActiveRulesSync()
        if (activeRules.isEmpty()) {
            db.ruleDao().insertRule(
                RuleEntity(
                    title = "Block Spam & Credit Card Offers",
                    keywordsCsv = "loan, credit card, insurance, investment, crypto, lottery, telemarketer",
                    responseText = "Thank you, but Sarkar is not interested in telemarketing or loans. Goodbye.",
                    actionType = CallActionType.DECLINE_AND_BLOCK,
                    isEnabled = true,
                    priority = 10
                )
            )
            db.ruleDao().insertRule(
                RuleEntity(
                    title = "Delivery Package Auto-Instruction",
                    keywordsCsv = "delivery, package, parcel, courier, amazon, swiggy, zomato, OTP",
                    responseText = "Please leave the parcel at the front door or guard room. I've sent a delivery notification to Sarkar.",
                    actionType = CallActionType.TAKE_MESSAGE,
                    isEnabled = true,
                    priority = 8
                )
            )
            db.ruleDao().insertRule(
                RuleEntity(
                    title = "Emergency & Urgent Priority",
                    keywordsCsv = "urgent, emergency, hospital, doctor, police, critical",
                    responseText = "Understood. High priority emergency detected. Connecting you to Sarkar right now.",
                    actionType = CallActionType.ACCEPT_AND_NOTIFY,
                    isEnabled = true,
                    priority = 9
                )
            )
        }
    }
}
