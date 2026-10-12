-- IPA dictionary imported from Wiktionary dumps (kaikki.org); one transcription per word.
create table pronunciation (
    lang            varchar(8)   not null,
    normalized_text varchar(200) not null,
    ipa             varchar(200) not null,
    source          varchar(20)  not null check (source in ('WIKTIONARY', 'ESPEAK', 'LLM')),
    primary key (lang, normalized_text)
);
