package pappel.database

import pappel.JSONUtils
import kotlin.js.Date
import kotlin.js.Promise

abstract class Model<T: Model.DataClass>(private val dataClass: T) {

    abstract val sequelizeModel: dynamic

    fun sync(force: Boolean = false, callback: ((error: Error?) -> Unit)? = null) {
        sequelizeModel.sync(JSONUtils.toJSON(mapOf("force" to force))).then {
            callback?.invoke(null)
        }.catch {
            err -> callback?.invoke(Error(err.message as String))
        }
    }

    fun new(): T {
        return ::dataClass.invoke()
    }

    fun save(data: T, callback: ((Error?) -> Unit)? = null) {
        val sequelizeData = sequelizeModel.build(data)
        sequelizeData.save().then {
            callback?.invoke(null)
        }.catch {
            err -> callback?.invoke(Error(err.message as String))
        }
    }

    fun findAll(callback: (List<T>) -> Unit) {
        sequelizeModel.findAll().then {
            records ->
            val list:MutableList<T> = mutableListOf()
            for (row in records) {
                list.add(hydrateFromInstance(row))
            }

            callback.invoke(list)
        }
    }

    fun findAll(): Promise<List<T>> {
        return Promise({
            resolve, reject ->
            sequelizeModel.findAll().then {
                records ->
                val list:MutableList<T> = mutableListOf()
                for (row in records) {
                    list.add(hydrateFromInstance(row))
                }

                resolve.invoke(list)
            }.catch {
                err -> reject.invoke(Error(err.message as String))
            }
            Unit
        })
    }

    private fun hydrateFromInstance(instance: dynamic): T {
        val data = new()

        val instanceKeys = (js("Object.keys(instance.dataValues)") as Array<String>).toSet()
        val dataKeys = (js("Object.keys(data)") as Array<String>).toSet()

        val keys = instanceKeys.intersect(dataKeys)

        for (key in keys) {
            js("data[key] = instance[key]")
        }

        return data
    }

    class FieldDefinition<T>(val options: Options<T>) {

        var value: T? = null

        // TODO: Add references
        // TODO: Add validation
        // TODO: Add custom getter and setter
        // TODO: Implement full sequelizer API

        class Options<T>(
                val name: String,
                val type: Type,
                val fieldName: String? = null,
                val allowNull: Boolean = true,
                val defaultValue: T? = null,
                val isPrimaryKey: Boolean = false,
                val isUnique: Boolean = false,
                val autoIncrement: Boolean = false,
                val comment: String? = null
        )

        enum class Type {
            INT, STRING
        }

    }

    open class DataClass(
            var createdAt: Date? = null,
            var updatedAt: Date? = null
    )

}