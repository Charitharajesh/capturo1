"""add_creator_followers_table

Revision ID: 77824a35d9fe
Revises: d8bad34bffb3
Create Date: 2026-06-01 00:51:21.526253

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '77824a35d9fe'
down_revision: Union[str, Sequence[str], None] = 'd8bad34bffb3'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        'creator_followers',
        sa.Column('id', sa.String(length=36), nullable=False),
        sa.Column('follower_id', sa.String(length=36), nullable=False),
        sa.Column('creator_id', sa.String(length=36), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['follower_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['creator_id'], ['creator_profiles.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('follower_id', 'creator_id', name='uq_follower_creator')
    )
    op.create_index('idx_followers_lookup', 'creator_followers', ['follower_id', 'creator_id'])


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_index('idx_followers_lookup', table_name='creator_followers')
    op.drop_table('creator_followers')
