package com.monster.literaryflow.data.db2

import androidx.room.TypeConverter
import com.monster.literaryflow.core.model.ActionType
import com.monster.literaryflow.core.model.ConditionType
import com.monster.literaryflow.core.model.FailurePolicy
import com.monster.literaryflow.core.model.MatchType
import com.monster.literaryflow.core.model.RecognitionSource
import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.StepType

class LiteraryFlow2TypeConverters {
    @TypeConverter fun fromScheduleType(value: ScheduleType): String = value.name
    @TypeConverter fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter fun fromStepType(value: StepType): String = value.name
    @TypeConverter fun toStepType(value: String): StepType = StepType.valueOf(value)

    @TypeConverter fun fromFailurePolicy(value: FailurePolicy): String = value.name
    @TypeConverter fun toFailurePolicy(value: String): FailurePolicy = FailurePolicy.valueOf(value)

    @TypeConverter fun fromActionType(value: ActionType): String = value.name
    @TypeConverter fun toActionType(value: String): ActionType = ActionType.valueOf(value)

    @TypeConverter fun fromConditionType(value: ConditionType): String = value.name
    @TypeConverter fun toConditionType(value: String): ConditionType = ConditionType.valueOf(value)

    @TypeConverter fun fromMatchType(value: MatchType?): String? = value?.name
    @TypeConverter fun toMatchType(value: String?): MatchType? = value?.let { MatchType.valueOf(it) }

    @TypeConverter fun fromRecognitionSource(value: RecognitionSource?): String? = value?.name
    @TypeConverter fun toRecognitionSource(value: String?): RecognitionSource? = value?.let { RecognitionSource.valueOf(it) }
}
