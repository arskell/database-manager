# database-manager
java and sqlite web-application for CRUD functions (часть моей производственной практики)


для компиляции - `./build.bat` 

для запуска - `./run.bat`


*Проект настроен на определенные таблицы. Для того чтобы добавить свои собственные, необходимо проделать ряд операций:*

1. Добавить следующую запись в HTTPServ.setup:

`table_fields.put("*ИМЯ_ТАБЛИЦЫ*", new ArrayList<String>());`\
`for(Field field: *ИМЯ_КЛАССА_С_ПОЛЯМИ_ТАБЛИЦЫ*.class.getFields()){`\
&nbsp;&nbsp;&nbsp;`table_fields.get(“*ИМЯ_ТАБЛИЦЫ*”).add(field.getName());`\
`}`  


2. В switch-case конструкцию в HTTPServ.createRecordObjectFromRequest:


`case "*ИМЯ_ТАБЛИЦЫ*":`\
`record = *ИМЯ_КЛАССА_С_ПОЛЯМИ_ТАБЛИЦЫ*();`\
`cl = *ИМЯ_КЛАССА_С_ПОЛЯМИ_ТАБЛИЦЫ*.class;`\
`break;`

3. И также настроить преобразования в ShopDb.normalizeRecordFromObject

4. Настроить файлы в database\create_queries\* и метод ShopDb.startProcedure

5. Изменить файл site\head_links.html для удобной навигации. Имя таблицы соответствует имени в url запросе  

*Некоторые особенности:*


1. Названия  полей в .sql файлах должны совпадать с названиями полей в соответствующих Record_*.java структурах
2. Первый столбец всегда id записи 
3. Поле с названием account_pass_hash при добавлении новой записи всегда хэшируется(очень слабой функцией String.hashCode, но это можно легко изменить)
4. Метод ShopDb.getCalculatorResults необходим был только для моей работы. При желании его можно удалить
