SELECT
    Person.ID AS pkidPerson,
    Fact.ID AS pkidFact,
    Fact.Text AS xy,
    (
        SELECT F.Text FROM Fact F WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Preferred = 1 AND
        F.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Name = 'Name')
        LIMIT 1
    ) name,
    (
        SELECT F.Text FROM Fact F WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Preferred = 1 AND
        F.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Name = 'Sex')
        LIMIT 1
    ) sex,
    (
        SELECT F.Date FROM Fact F WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Preferred = 1 AND
        F.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Name = 'Birth')
        LIMIT 1
    ) birth,
    (
        SELECT P.Name FROM Fact F INNER JOIN Place P ON (P.ID = F.PlaceID)
        WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Preferred = 1 AND
        F.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Name = 'Birth')
        LIMIT 1
    ) birthplace,
    (
        SELECT Name FROM (
        SELECT F.Date, P.Name FROM Fact F INNER JOIN Place P ON (P.ID = F.PlaceID)
        WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Date IS NOT NULL UNION
        SELECT F.Date, P.Name FROM Relationship R INNER JOIN Fact F ON (F.LinkID = R.ID AND F.LinkTableID = 7) INNER JOIN Place P ON (P.ID = F.PlaceID)
        WHERE (R.Person1ID = Person.ID OR R.Person2ID = Person.ID) AND F.Date IS NOT NULL ORDER BY Date LIMIT 1)
    ) anyplace,
    (
        SELECT F.Date FROM Fact F WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Preferred = 1 AND
        F.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Name = 'Death')
        LIMIT 1
    ) death
FROM
    Person LEFT OUTER JOIN
    Fact ON (
        Fact.LinkTableID = 5 AND Fact.LinkID = Person.ID AND Fact.Preferred = 1 AND
        Fact.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Abbreviation = '_XY'))
