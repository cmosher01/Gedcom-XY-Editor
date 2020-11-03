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
        SELECT F.Date FROM Fact F WHERE F.LinkTableID = 5 AND F.LinkID = Person.ID AND F.Preferred = 1 AND
        F.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Name = 'Death')
        LIMIT 1
    ) death
FROM
    Person LEFT OUTER JOIN
    Fact ON (
        Fact.LinkTableID = 5 AND Fact.LinkID = Person.ID AND Fact.Preferred = 1 AND
        Fact.FactTypeID IN (SELECT ID FROM FactType WHERE FactType.Abbreviation = '_XY'))
