from sqlalchemy import Column, String, Text, ForeignKey, Integer, Boolean, Date, Table
from sqlalchemy.orm import relationship
from .db import Base



class GDPRArticle(Base):
        __tablename__ = "gdpr_articles"

        id = Column(Integer, primary_key=True)
        article_number = Column(String(2), nullable=False)
        paragraph = Column(String(2), nullable=True)
        literature = Column(String(2), nullable=True)

        text = Column(Text, nullable=False)

        criteria = relationship("GDPRCriteria", secondary="criterion_article_association", back_populates="articles")

criterion_article_association = Table(
    "criterion_article_association",
    Base.metadata,
    Column("criterion_id", Integer, ForeignKey("gdpr_criteria.id"), primary_key=True),
    Column("article_id", Integer, ForeignKey("gdpr_articles.id"), primary_key=True)
)

category_reason_association = Table(
    "category_reason_association",
    Base.metadata,

    Column(
        "category_id",
        Integer,
        ForeignKey("category.id"),
        primary_key=True
    ),

    Column(
        "reason_id",
        Integer,
        ForeignKey("reason.id"),
        primary_key=True
    )
)

class Category(Base):
    __tablename__ = "category"

    id = Column(Integer, primary_key=True)
    name = Column(String(255), unique=True, nullable=False)

    criteria = relationship(
        "GDPRCriteria",
        back_populates="category"
    )

    reasons = relationship(
        "Reason",
        secondary=category_reason_association,
        back_populates="criteria"
    )

class GDPRCriteria(Base):
    __tablename__ = "gdpr_criteria"

    id = Column(Integer, primary_key=True)
    category_id = Column(Integer, ForeignKey("category.id"), nullable=False)

    short_name = Column(String(255), nullable=False)
    description = Column(Text, nullable=False)

    category = relationship(
        "Category",
        back_populates="criteria"
    )

    articles = relationship(
        "GDPRArticle",
        secondary="criterion_article_association",
        back_populates="criteria"
    )

class Reason(Base):
        __tablename__ = "reason"

        id = Column(Integer, primary_key=True)

        reason = Column(
            Text,
            nullable=False,
            unique=True
        )

        categories = relationship(
            "Category",
            secondary=category_reason_association,
            back_populates="categories"
        )

class FallbackLLMModel(Base):
        __tablename__ = "fallback_llm"
        id = Column(Integer, primary_key=True)
        corresponding_comment = Column(Text, nullable=False)
        name = Column(String(255), nullable=False)
        order = Column(Integer, nullable=False)
        model_url = Column(Text, nullable=False)
        env_api_key_name = Column(Text, nullable=False)
